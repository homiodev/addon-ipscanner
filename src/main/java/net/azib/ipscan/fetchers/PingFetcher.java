/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
 */
package net.azib.ipscan.fetchers;

import static net.azib.ipscan.core.ScanningSubject.PARAMETER_PING_RESULT;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.ScanningResult.ResultType;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.core.net.PingResult;
import net.azib.ipscan.core.net.Pinger;
import net.azib.ipscan.core.net.PingerRegistry;
import net.azib.ipscan.core.values.IntegerWithUnit;
import org.jetbrains.annotations.NotNull;

/**
 * PingFetcher is able to ping IP addresses.
 * It returns the average round trip time of all pings sent.
 *
 * @author Anton Keks
 */
@Log4j2
@RequiredArgsConstructor
public class PingFetcher implements Fetcher {

	/** The shared pinger - this one must be static, because PingTTLFetcher will use it as well */
	private static volatile Pinger pinger;
	private static final AtomicInteger pingerUsers = new AtomicInteger();

	/** The registry used for creation of Pinger instances */
	private final PingerRegistry pingerRegistry;
	private final ScannerConfig config;

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.Ping;
	}

	protected PingResult executePing(ScanningSubject subject) {
		if (subject.hasParameter(PARAMETER_PING_RESULT))
			return (PingResult) subject.getParameter(PARAMETER_PING_RESULT);

		PingResult result;
		try {
			result = pinger.ping(subject, config.pingCount);
		}
		catch (IOException e) {
			// if this is not a timeout
			log.warn("Pinging failed", e);
			// return an empty ping result
			result = new PingResult(subject.getAddress(), 0);
		}
		// remember the result for other fetchers to use
		subject.setParameter(PARAMETER_PING_RESULT, result);
		return result;
	}

	public Object scan(ScanningSubject subject) {
		PingResult result = executePing(subject);
		subject.setResultType(result.isAlive() ? ResultType.ALIVE : ResultType.DEAD);

		if (!result.isAlive() && !config.scanDeadHosts) {
			// the host is dead, we are not going to continue...
			subject.abortAddressScanning();
		}

		return result.isAlive() ? new IntegerWithUnit(result.getAverageTime(), "ms") : null;
	}

	public void init() {
		if (pinger == null) {
			pinger = pingerRegistry.createPinger();
			pingerUsers.set(1);
		}
		else
			pingerUsers.incrementAndGet();
	}

	public void cleanup() {
		try {
			if (pingerUsers.decrementAndGet() <= 0 && pinger != null) {
				pinger.close();
				pinger = null;
			}
		}
		catch (IOException e) {
			pinger = null;
			throw new FetcherException(e);
		}
	}
}
