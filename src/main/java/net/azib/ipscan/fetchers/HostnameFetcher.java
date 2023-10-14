/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
 */
package net.azib.ipscan.fetchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.util.MDNSResolver;
import net.azib.ipscan.util.NetBIOSResolver;
import org.jetbrains.annotations.NotNull;

/**
 * HostnameFetcher retrieves hostnames of IP addresses by reverse DNS lookups.
 *
 * @author Anton Keks
 */
@Log4j2
public class HostnameFetcher implements Fetcher {

	private static Object inetAddressImpl;
	private static Method getHostByAddr;

	static {
		try {
			Field impl = InetAddress.class.getDeclaredField("impl");
			impl.setAccessible(true);
			inetAddressImpl = impl.get(null);
			getHostByAddr = inetAddressImpl.getClass().getDeclaredMethod("getHostByAddr", byte[].class);
			getHostByAddr.setAccessible(true);
		}
		catch (Exception e) {
			log.warn("Could not get InetAddressImpl", e);
		}
	}

	public HostnameFetcher() {}

    @Override
    public @NotNull IPScannerService.Fetcher getFetcherID() {
        return IPScannerService.Fetcher.Hostname;
    }

	@SuppressWarnings("PrimitiveArrayArgumentToVariableArgMethod")
	private String resolveWithRegularDNS(InetAddress ip) {
		try {
			// faster way to do lookup - getCanonicalHostName() actually does both reverse and forward lookups inside
			return (String) getHostByAddr.invoke(inetAddressImpl, ip.getAddress());
		}
		catch (Exception e) {
			if (e instanceof InvocationTargetException && e.getCause() instanceof UnknownHostException)
				return null;

			// return the returned hostname only if it is not the same as the IP address (this is how the above method works)
			String hostname = ip.getCanonicalHostName();
			return ip.getHostAddress().equals(hostname) ? null : hostname;
		}
	}

	private String resolveWithMulticastDNS(ScanningSubject subject) {
		try {
			MDNSResolver resolver = new MDNSResolver(subject.getAdaptedPortTimeout());
			String name = resolver.resolve(subject.getAddress());
			resolver.close();
			return name;
		}
		catch (SocketTimeoutException | SocketException e) {
			return null;
		}
		catch (Exception e) {
			log.warn("Failed to query mDNS for " + subject, e);
			return null;
		}
	}

	private String resolveWithNetBIOS(ScanningSubject subject) {
		try {
			NetBIOSResolver resolver = new NetBIOSResolver(subject.getAdaptedPortTimeout());
			String[] names = resolver.resolve(subject.getAddress());
			resolver.close();
			return names == null ? null : names[0];
		}
		catch (SocketTimeoutException | SocketException e) {
			return null;
		}
		catch (Exception e) {
			log.warn("Failed to query NetBIOS for " + subject, e);
			return null;
		}
	}

	public Object scan(ScanningSubject subject) {
		String name = resolveWithRegularDNS(subject.getAddress());
		if (name == null && subject.isLocal()) name = resolveWithMulticastDNS(subject);
		if (name == null && subject.isLocal()) name = resolveWithNetBIOS(subject);
		return name;
	}
}
