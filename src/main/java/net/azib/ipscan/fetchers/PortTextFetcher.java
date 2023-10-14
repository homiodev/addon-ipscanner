/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.fetchers;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static net.azib.ipscan.fetchers.PortsFetcher.PARAMETER_OPEN_PORTS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.ScanningResult.ResultType;
import net.azib.ipscan.core.ScanningSubject;

/**
 * PortTextFetcher - generic configurable fetcher to read some particular information from a port.
 *
 * @author Anton Keks
 */
@Log4j2
public abstract class PortTextFetcher implements Fetcher {

	private final ScannerConfig scannerConfig;

	private final int defaultPort;
	protected boolean scanOpenPorts;
	@Getter protected String textToSend;
	@Getter protected Pattern matchingRegexp;
	@Getter protected int extractGroup;

	public PortTextFetcher(ScannerConfig scannerConfig, int defaultPort, String defaultTextToSend, String matchingRegexp) {
		this.scannerConfig = scannerConfig;
		this.defaultPort = defaultPort;
		this.textToSend = defaultTextToSend;
		this.matchingRegexp = Pattern.compile(matchingRegexp);
		this.extractGroup = 1;
	}

	public Object scan(ScanningSubject subject) {
		Iterator<Integer> portIterator = getPortIterator(subject);

		while (portIterator.hasNext() && !currentThread().isInterrupted()) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(subject.getAddress(), portIterator.next()), subject.getAdaptedPortTimeout());
				socket.setTcpNoDelay(true);
				socket.setSoTimeout(scannerConfig.portTimeout * 2);
				socket.setSoLinger(true, 0);

				socket.getOutputStream().write(textToSend.getBytes());

				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					Matcher matcher = matchingRegexp.matcher(line);
					if (matcher.find()) {
						// mark that additional info is available
						subject.setResultType(ResultType.WITH_PORTS);
						// return the required contents
						return getResult(matcher, socket.getPort());
					}
				}
			}
			catch (ConnectException e) {
				// no connection
			}
			catch (SocketTimeoutException e) {
				// no information
			}
			catch (SocketException e) {
				// connection reset
			}
			catch (IOException e) {
				log.debug(subject.getAddress().toString(), e);
			}
		}
		return null;
	}

    protected String getResult(Matcher matcher, int port) {
		String result = matcher.group(extractGroup);
		return result.isEmpty() ? String.valueOf(port) : result;
	}

	private Iterator<Integer> getPortIterator(ScanningSubject subject) {
		if (scanOpenPorts) {
			@SuppressWarnings("unchecked")
			SortedSet<Integer> openPorts = (SortedSet<Integer>) subject.getParameter(PARAMETER_OPEN_PORTS);
			if (openPorts != null) {
				SortedSet<Integer> ports = new TreeSet<>(openPorts);
				ports.add(defaultPort);
				return ports.iterator();
			}
		}
		return subject.isAnyPortRequested() ? subject.requestedPortsIterator() : singleton(defaultPort).iterator();
	}
}
