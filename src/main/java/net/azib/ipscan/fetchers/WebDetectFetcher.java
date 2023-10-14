/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */

package net.azib.ipscan.fetchers;

import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.ScannerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * WebDetectFetcher - detects the Web server software running on scanned hosts.
 *
 * @author Anton Keks
 */
public class WebDetectFetcher extends PortTextFetcher {

	public WebDetectFetcher(ScannerConfig scannerConfig) {
		super(scannerConfig, 80, "HEAD /robots.txt HTTP/1.0\r\n\r\n", "^[Ss]erver:\\s+(.*)$");
	}

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.WebDetect;
	}
}
