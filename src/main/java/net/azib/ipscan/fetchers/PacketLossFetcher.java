package net.azib.ipscan.fetchers;

import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.ScannerConfig;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.core.net.PingResult;
import net.azib.ipscan.core.net.PingerRegistry;
import org.jetbrains.annotations.NotNull;

import static net.azib.ipscan.core.ScanningResult.ResultType.ALIVE;
import static net.azib.ipscan.core.ScanningResult.ResultType.DEAD;

/**
 * PacketLossFetcher shares pinging results with PingFetcher
 * and returns the packet loss field of the received packet.
 *
 * @author Gustavo Pistore
 */
public class PacketLossFetcher extends PingFetcher {

	public PacketLossFetcher(PingerRegistry pingerRegistry, ScannerConfig scannerConfig) {
		super(pingerRegistry, scannerConfig);
	}

	@Override
	public @NotNull IPScannerService.Fetcher getFetcherID() {
		return IPScannerService.Fetcher.PacketLoss;
	}

	public Object scan(ScanningSubject subject) {
		PingResult result = executePing(subject);
		subject.setResultType(result.isAlive() ? ALIVE : DEAD);

		return result.getPacketLoss() + "/" + result.getPacketCount() + " (" + result.getPacketLossPercent() + "%)";
	}
}
