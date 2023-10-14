/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core.net;

import static java.lang.Math.min;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.core.ScanningSubject;

/**
 * TCP Pinger. Uses a TCP port to ping, doesn't require root privileges.
 *
 * @author Anton Keks
 */
@Log4j2
@RequiredArgsConstructor
public class TCPPinger implements Pinger {

	// try different ports in sequence, starting with 80 (which is most probably not filtered)
	private static final int[] PROBE_TCP_PORTS = {80, 7, 443, 139, 22};

	private final int timeout;

	public PingResult ping(ScanningSubject subject, int count) {
		PingResult result = new PingResult(subject.getAddress(), count);
		int workingPort = -1;

		Socket socket;
		for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
			socket = new Socket();
			// cycle through different ports until a working one is found
			int probePort = workingPort >= 0 ? workingPort : PROBE_TCP_PORTS[i % PROBE_TCP_PORTS.length];
			// change the first port to the requested one, if it is available
			if (i == 0 && subject.isAnyPortRequested())
				probePort = subject.requestedPortsIterator().next();

			long startTime = System.currentTimeMillis();
			try {
				// set some optimization options
				socket.setReuseAddress(true);
				socket.setReceiveBufferSize(32);
				int timeout = result.isTimeoutAdaptationAllowed() ? min(result.getLongestTime() * 2, this.timeout) : this.timeout;
				socket.connect(new InetSocketAddress(subject.getAddress(), probePort), timeout);
				if (socket.isConnected()) {
					// it worked - success
					success(result, startTime);
					// it worked! - remember the current port
					workingPort = probePort;
				}
			}
			catch (SocketTimeoutException ignore) {
			}
			catch (IOException e) {
				String msg = e.getMessage();

				// RST should result in ConnectException, but on macOS ConnectionException can also come with e.g. "No route to host"
				if (msg.contains(/*Connection*/"refused")) {
					// we've got an RST packet from the host - it is alive
					success(result, startTime);
				}
				// this should result in NoRouteToHostException or ConnectException, but not all Java implementation respect that
				else if (e instanceof NoRouteToHostException || msg.contains(/*No*/"route to host") || msg.contains(/*Host is*/"down") || msg.contains(/*Network*/"unreachable") || msg.contains(/*Socket*/"closed") || msg.contains("Invalid argument")) {
					// host is down
					break;
				}
				else {
					// something unknown
					log.info(subject.toString(), e);
				}
			}
			finally {
				closeQuietly(socket);
			}
		}

		return result;
	}

	private void success(PingResult result, long startTime) {
		result.addReply(System.currentTimeMillis() - startTime);
		// one positive result is enough for TCP
		result.enableTimeoutAdaptation();
	}

	public void close() {
	}
}
