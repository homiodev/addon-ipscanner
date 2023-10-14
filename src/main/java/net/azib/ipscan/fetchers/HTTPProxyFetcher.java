/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */

package net.azib.ipscan.fetchers;

import java.util.regex.Matcher;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.ScannerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * HTTPProxyFetcher - detects HTTP proxy on port 3128 (or other requested port)
 *
 * @author Anton Keks
 */
public class HTTPProxyFetcher extends PortTextFetcher {
	public HTTPProxyFetcher(ScannerConfig scannerConfig) {
		super(scannerConfig, 3128, "HEAD http://www.google.com HTTP/1.0\r\n\r\n", "^(HTTP/[\\d\\.]+ [23].*)$");
		this.scanOpenPorts = true;
	}

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.HttpProxy;
	}

	@Override
	protected String getResult(Matcher matcher, int port) {
		return port + ": " + super.getResult(matcher, port);
	}
}
