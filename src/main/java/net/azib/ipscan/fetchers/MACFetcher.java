package net.azib.ipscan.fetchers;

import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningSubject;
import org.jetbrains.annotations.NotNull;

public abstract class MACFetcher implements Fetcher {

    static final Pattern macAddressPattern = Pattern.compile("([a-fA-F0-9]{1,2}[-:]){5}[a-fA-F0-9]{1,2}");
    static final Pattern leadingZeroesPattern = Pattern.compile("(?<=^|-|:)([A-F0-9])(?=-|:|$)");

    @Override
    public @NotNull IPScannerService.Fetcher getFetcherID() {
        return IPScannerService.Fetcher.MAC;
    }

    @Override
    public final String scan(ScanningSubject subject) {
        String mac = (String) subject.getParameter(IPScannerService.Fetcher.MAC.name());
			if (mac == null) {mac = resolveMAC(subject.getAddress());}
        subject.setParameter(IPScannerService.Fetcher.MAC.name(), mac);
        return mac;
    }

    protected abstract String resolveMAC(InetAddress address);

    String bytesToMAC(byte[] bytes) {
        StringBuilder mac = new StringBuilder();
			for (byte b : bytes) {mac.append(String.format("%02X", b)).append(":");}
			if (!mac.isEmpty()) {mac.deleteCharAt(mac.length() - 1);}
        return mac.toString();
    }

    String extractMAC(String line) {
        Matcher m = macAddressPattern.matcher(line);
        return m.find() ? addLeadingZeroes(m.group().toUpperCase()) : null;
    }

    private static String addLeadingZeroes(String mac) {
        return leadingZeroesPattern.matcher(mac).replaceAll("0$1");
    }
}
