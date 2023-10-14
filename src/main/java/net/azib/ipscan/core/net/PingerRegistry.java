/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.core.net;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.fetchers.FetcherException;
import org.apache.commons.lang3.SystemUtils;

/**
 * PingerRegistry
 *
 * @author Anton Keks
 */
@Log4j2
public class PingerRegistry {

	private final ScannerConfig scannerConfig;

	/** All available Pinger implementations */
	Map<String, Class<? extends Pinger>> pingers;

	@SuppressWarnings("unchecked")
	public PingerRegistry(ScannerConfig scannerConfig) throws ClassNotFoundException {
		this.scannerConfig = scannerConfig;

		pingers = new LinkedHashMap<>();
		if (SystemUtils.IS_OS_WINDOWS)
			pingers.put("pinger.windows", (Class<Pinger>) Class.forName(getClass().getPackage().getName() + ".WindowsPinger"));
		if (SystemUtils.IS_OS_LINUX && SystemUtils.OS_ARCH.contains("64"))
			pingers.put("pinger.icmp", ICMPSharedPinger.class);
		pingers.put("pinger.udp", UDPPinger.class);
		pingers.put("pinger.tcp", TCPPinger.class);
		pingers.put("pinger.combined", CombinedUnprivilegedPinger.class);
		pingers.put("pinger.java", JavaPinger.class);
	}

	/**
	 * Creates the configured pinger with configured timeout
	 */
	public Pinger createPinger() throws FetcherException {
		return createPinger(scannerConfig.selectedPinger, scannerConfig.pingTimeout);
	}

	/**
	 * Creates a specified pinger with specified timeout
	 */
	Pinger createPinger(String pingerName, int timeout) throws FetcherException {
		Class<? extends Pinger> pingerClass = pingers.get(pingerName);
		Constructor<? extends Pinger> constructor;
		try {
			constructor = pingerClass.getConstructor(int.class);
			return constructor.newInstance(timeout);
		}
		catch (Exception e) {
			Throwable t = e instanceof InvocationTargetException ? e.getCause() : e;
			String message = "Unable to create pinger: " + pingerName;
			log.error(message, t);
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			throw new FetcherException("pingerCreateFailure");
		}
	}

	public boolean checkSelectedPinger() {
		// this method must be fast, so we are not checking all the implementations
		// currently only icmp pingers may not be supported, so let's check them
		if (scannerConfig.selectedPinger.startsWith("pinger.icmp")) {
			try {
				try (Pinger icmpPinger = createPinger(scannerConfig.selectedPinger, 250)) {
					icmpPinger.ping(new ScanningSubject(InetAddress.getLocalHost(), scannerConfig), 1);
				}
			}
			catch (Throwable e) {
				log.info("ICMP pinger failed: " + e);
				// win32 will use native pinger, all others get combined UDP+TCP, which doesn't require special privileges
				scannerConfig.selectedPinger = SystemUtils.IS_OS_WINDOWS ? "pinger.windows" :
					SystemUtils.IS_OS_MAC ? "pinger.java" : "pinger.combined";
				return false;
			}
		}
		return true;
	}
}
