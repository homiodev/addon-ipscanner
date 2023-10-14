package net.azib.ipscan.fetchers;

import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningSubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MACVendorFetcher implements Fetcher {
	private static final Map<String, String> vendors = new HashMap<>();
	private final MACFetcher macFetcher;

	public MACVendorFetcher(MACFetcher macFetcher) {
		this.macFetcher = macFetcher;
	}

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.MACVendor;
	}

	@Override
	public void init() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/mac-vendors.txt")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				vendors.put(line.substring(0, 6), line.substring(6));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object scan(ScanningSubject subject) {
		String mac = (String)subject.getParameter(IPScannerService.Fetcher.MAC.name());
		if (mac == null) {
			macFetcher.scan(subject);
			mac = (String) subject.getParameter(IPScannerService.Fetcher.MAC.name());
		}
		return mac != null ? findMACVendor(mac) : null;
	}

	String findMACVendor(String mac) {
		return vendors.get(mac.replace(":", "").substring(0, 6));
	}
}
