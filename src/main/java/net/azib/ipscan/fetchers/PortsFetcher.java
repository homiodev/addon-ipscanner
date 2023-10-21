/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.fetchers;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.PortIterator;
import net.azib.ipscan.core.ScanningResult.ResultType;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.core.values.NotScanned;
import net.azib.ipscan.core.values.NumericRangeList;
import net.azib.ipscan.util.SequenceIterator;
import net.azib.ipscan.util.ThreadResourceBinder;
import org.homio.hquery.ProgressBar;
import org.jetbrains.annotations.NotNull;

/**
 * PortsFetcher scans TCP ports.
 * Port list is obtained using the {@link net.azib.ipscan.core.PortIterator}.
 *
 * @author Anton Keks
 */
public class PortsFetcher implements Fetcher {

	static final String PARAMETER_OPEN_PORTS = "openPorts";
	static final String PARAMETER_FILTERED_PORTS = "filteredPorts";

	private final ScannerConfig config;
	private final ThreadResourceBinder<Socket> sockets = new ThreadResourceBinder<>();

	// initialize preferences for this scan
	private PortIterator portIteratorPrototype;
	protected boolean displayAsRanges = true;	// TODO: make configurable

	public PortsFetcher(ScannerConfig scannerConfig) {
		this.config = scannerConfig;
	}

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.Ports;
	}

	@Override
	public String getFullName() {
		int numPorts = new PortIterator(config.portString).size();
		return getName() + " [" + numPorts + (config.useRequestedPorts ? "+" : "" ) + "]";
	}

	/**
	 * This method does the actual port scanning.
	 * It then remembers the results for other extending fetchers to use, like FilteredPortsFetcher.
	 * @param subject the address to scan
	 * @return true if any ports were scanned, false otherwise
	 */
	@SuppressWarnings("unchecked")
	protected boolean scanPorts(ScanningSubject subject) {
		SortedSet<Integer> openPorts = getOpenPorts(subject);

		if (openPorts == null) {
			// no results are available yet, let's proceed with the scanning
			openPorts = new TreeSet<>();
			SortedSet<Integer> filteredPorts = new TreeSet<>();
			subject.setParameter(PARAMETER_OPEN_PORTS, openPorts);
			subject.setParameter(PARAMETER_FILTERED_PORTS, filteredPorts);

			int portTimeout = subject.getAdaptedPortTimeout();

			// clone port iterator for performance instead of creating for every thread
			Iterator<Integer> portsIterator = portIteratorPrototype.copy();
			if (config.useRequestedPorts && subject.isAnyPortRequested()) {
				// add requested ports to the iteration
				portsIterator = new SequenceIterator<>(portsIterator, subject.requestedPortsIterator());
			}
			if (!portsIterator.hasNext()) {
				// no ports are configured for scanning
				return false;
			}

			int size = portIteratorPrototype.size();
			String address = subject.getAddress().getHostAddress();
			ProgressBar progressBar = this.config.context.ui().progress().createProgressBar(address
				+ "ipscanner-ports", size < 3, () -> {});
			double delta = 100D / size, progress = 0D;

			try {
				while (portsIterator.hasNext() && !Thread.currentThread().isInterrupted()) {
					// TODO: UDP ports?
					Socket socket = sockets.bind(new Socket());
					int port = portsIterator.next();
					try {
						if (progressBar.isCancelled()) {
							return true;
						}
						progressBar.progress(progress, "%s [%d]".formatted(address, port));
						progress += delta;
						// set some optimization options
						socket.setReuseAddress(true);
						socket.setReceiveBufferSize(32);
						// now connect
						socket.connect(new InetSocketAddress(subject.getAddress(), port), portTimeout);
						// some more options
						socket.setSoLinger(true, 0);
						socket.setSendBufferSize(16);
						socket.setTcpNoDelay(true);

						if (socket.isConnected()) {openPorts.add(port);}
					} catch (SocketTimeoutException e) {
						filteredPorts.add(port);
					} catch (IOException e) {
						// connection refused
						assert e instanceof ConnectException : e;
					} finally {
						sockets.closeAndUnbind(socket);
					}
				}
			} finally {
				progressBar.done();
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	protected SortedSet<Integer> getOpenPorts(ScanningSubject subject) {
		return (SortedSet<Integer>) subject.getParameter(PARAMETER_OPEN_PORTS);
	}

	public Object scan(ScanningSubject subject) {
		boolean portsScanned = scanPorts(subject);
		if (!portsScanned)
			return NotScanned.VALUE;

		SortedSet<Integer> openPorts = getOpenPorts(subject);
		if (!openPorts.isEmpty()) {
			subject.setResultType(ResultType.WITH_PORTS);
			return new NumericRangeList(openPorts, displayAsRanges);
		}
		return null;
	}

	public void init() {
		// rebuild port iterator before each scan
		this.portIteratorPrototype = new PortIterator(config.portString);
	}

  @Override
  public void cleanup() {
    sockets.close();
  }
}
