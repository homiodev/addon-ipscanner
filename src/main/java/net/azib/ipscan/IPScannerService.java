package net.azib.ipscan;

import static java.util.Arrays.asList;
import static net.azib.ipscan.core.state.ScanningState.IDLE;
import static org.homio.addon.ipscanner.IPScanResultConsolePlugin.PLUGIN_NAME;

import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import net.azib.ipscan.core.Scanner;
import net.azib.ipscan.core.ScannerDispatcherThread;
import net.azib.ipscan.core.ScanningProgressCallback;
import net.azib.ipscan.core.ScanningResult;
import net.azib.ipscan.core.ScanningResultCallback;
import net.azib.ipscan.core.ScanningResultList;
import net.azib.ipscan.core.net.PingerRegistry;
import net.azib.ipscan.core.state.ScanningState;
import net.azib.ipscan.core.state.StateMachine;
import net.azib.ipscan.core.state.StateTransitionListener;
import net.azib.ipscan.feeders.RangeFeeder;
import net.azib.ipscan.fetchers.FetcherRegistry;
import net.azib.ipscan.fetchers.HTTPProxyFetcher;
import net.azib.ipscan.fetchers.HTTPSenderFetcher;
import net.azib.ipscan.fetchers.HostnameFetcher;
import net.azib.ipscan.fetchers.MACFetcher;
import net.azib.ipscan.fetchers.MACVendorFetcher;
import net.azib.ipscan.fetchers.NetBIOSInfoFetcher;
import net.azib.ipscan.fetchers.PacketLossFetcher;
import net.azib.ipscan.fetchers.PingFetcher;
import net.azib.ipscan.fetchers.PingTTLFetcher;
import net.azib.ipscan.fetchers.PortsFetcher;
import net.azib.ipscan.fetchers.UnixMACFetcher;
import net.azib.ipscan.fetchers.WebDetectFetcher;
import net.azib.ipscan.fetchers.WinMACFetcher;
import org.apache.commons.lang3.SystemUtils;
import org.homio.api.Context;
import org.homio.hquery.ProgressBar;
import org.jetbrains.annotations.Nullable;

public class IPScannerService implements ScanningProgressCallback, ScanningResultCallback, StateTransitionListener {

	static {
		Security.setProperty("networkaddress.cache.ttl", "0");
		Security.setProperty("networkaddress.cache.negative.ttl", "0");
	}

	private final PingerRegistry pingerRegistry;
	private final Scanner scanner;
	@Getter private final StateMachine stateMachine;
	@Getter private final ScannerConfig scannerConfig;
	@Getter private final IPScannerContext ipScannerContext;
	private final Context context;
	@Getter private final FetcherRegistry fetcherRegistry;

	private ScannerDispatcherThread scannerThread;
	private RangeFeeder feeder;
	private Runnable completeHandler = () -> {
	};
	private final Consumer<StateMachine.Transition> startHandler = transition -> {
	};

	@SneakyThrows
	public IPScannerService(Context context) {
		this.context = context;
		this.scannerConfig = new ScannerConfig();
		this.pingerRegistry = new PingerRegistry(scannerConfig);

		this.stateMachine = new StateMachine() {
		};
		this.stateMachine.addTransitionListener(this);

		this.fetcherRegistry = buildFetcherRegistry();
		this.scanner = new Scanner(fetcherRegistry);
		this.ipScannerContext = new IPScannerContext(fetcherRegistry);
		this.stateMachine.init();
	}

	private FetcherRegistry buildFetcherRegistry() {
		MACFetcher macFetcher = SystemUtils.IS_OS_WINDOWS ? new WinMACFetcher() : new UnixMACFetcher();
		return new FetcherRegistry(asList(
			new PingFetcher(pingerRegistry, scannerConfig),
			new PingTTLFetcher(pingerRegistry, scannerConfig),
			new HostnameFetcher(),
			new WebDetectFetcher(scannerConfig),
			new HTTPProxyFetcher(scannerConfig),
			new HTTPSenderFetcher(scannerConfig),
			new PacketLossFetcher(pingerRegistry, scannerConfig),
			new NetBIOSInfoFetcher(),
			new PortsFetcher(scannerConfig),
			new MACVendorFetcher(macFetcher),
			macFetcher));
	}

	@Override
	public void updateProgress(@Nullable String msg, int runningThreads, double percentageComplete) {
		msg = msg == null ? "IPScanner: active tasks: [%s]".formatted(runningThreads) : msg;
		scannerConfig.progressBar.progress(percentageComplete, msg);
		context.ui().console().refreshPluginContent(PLUGIN_NAME,
			context.ui().console().getRegisteredPlugin(PLUGIN_NAME).getValue());
	}

	@Override
	public void prepareForResults(ScanningResult result) {
		if (this.ipScannerContext.scanningResults.isRegistered(result)) {
			// just redraw the item
			this.ipScannerContext.scanningResults.update(result);
		}
		else {
			this.ipScannerContext.scanningResults.registerAtIndex(0, result);
		}
	}

	@Override
	public void consumeResults(ScanningResult result) {
		this.prepareForResults(result);
	}

	private ScannerDispatcherThread createScannerThread(RangeFeeder feeder, ScanningProgressCallback progressCallback, ScanningResultCallback resultsCallback) {
		return new ScannerDispatcherThread(feeder, scanner, stateMachine, progressCallback, this.ipScannerContext.scanningResults, scannerConfig, resultsCallback);
	}

	public void startScan(String startIP, String endIP, String ports, ProgressBar progressBar) {
		scannerConfig.portString = ports;
		scannerConfig.progressBar = progressBar;
		scannerConfig.context = context;
		feeder = new RangeFeeder(startIP, endIP, scannerConfig);
		if (stateMachine.inState(IDLE)) {
			if (!this.pingerRegistry.checkSelectedPinger())
				throw new IllegalStateException("Unable to start ip scanner");
		}
		stateMachine.transitionToNext();
	}

	@Override
	public void transitionTo(ScanningState state, StateMachine.Transition transition) {
		this.ipScannerContext.state = transition;
		switch (state) {
			case STARTING:
			case RESTARTING:
				if (transition != StateMachine.Transition.CONTINUE) {
					this.ipScannerContext.clear();
				}
				try {
					scannerThread = createScannerThread(feeder, this, this);
					stateMachine.startScanning();
				}
				catch (RuntimeException e) {
					stateMachine.reset();
					throw e;
				}
				break;
			case SCANNING:
				scannerThread.start();
				break;
		}
		switch (transition) {
			case START:
			case RESCAN:
				startHandler.accept(transition);
				break;
			case COMPLETE:
				completeHandler.run();
				break;
		}
	}

	public void setCompleteHandler(Runnable completeHandler) {
		this.completeHandler = completeHandler;
	}

	public static final class IPScannerContext {
		public StateMachine.Transition state;
		public final ScanningResultList scanningResults;

		public IPScannerContext(FetcherRegistry fetcherRegistry) {
			this.scanningResults = new ScanningResultList(fetcherRegistry);
		}

		public void clear() {
			scanningResults.clear();
		}

		public List<ResultValue> getScanningResults() {
			if (scanningResults.getFetchers() != null) {

				List<ScanningResult> snap = new ArrayList<>(scanningResults.getResultList());
				List<Integer> indexes = Stream.of(Fetcher.values()).map(scanningResults::getFetcherIndex).toList();
				return snap.stream().map(s -> new ResultValue(s, indexes)).toList();
			}
			return Collections.emptyList();
		}
	}

	public static class ResultValue {
		public final String hostname;
		public final String address;
		public final String ping;
		public final String webDetectValue;
		public final String httpSenderValue;
		public final String netBIOSInfo;
		public final String ports;
		public final String macVendorValue;
		public final String macFetcherValue;
		public final ScanningResult.ResultType type;

		public ResultValue(ScanningResult scanningResult, List<Integer> indexes) {
			this.address = scanningResult.getAddress().getHostAddress();
			this.type = scanningResult.getType();
			this.ping = nullSafeValue(scanningResult, indexes.get(Fetcher.Ping.ordinal()));
			this.hostname = nullSafeValue(scanningResult, indexes.get(Fetcher.Hostname.ordinal()));
			this.webDetectValue = nullSafeValue(scanningResult, indexes.get(Fetcher.WebDetect.ordinal()));
			this.httpSenderValue = nullSafeValue(scanningResult, indexes.get(Fetcher.HTTPSender.ordinal()));
			this.netBIOSInfo = nullSafeValue(scanningResult, indexes.get(Fetcher.NetBIOSInfo.ordinal()));
			this.ports = nullSafeValue(scanningResult, indexes.get(Fetcher.Ports.ordinal()));
			this.macVendorValue = nullSafeValue(scanningResult, indexes.get(Fetcher.MACVendor.ordinal()));
			this.macFetcherValue = nullSafeValue(scanningResult, indexes.get(Fetcher.MAC.ordinal()));
		}

		private String nullSafeValue(ScanningResult scanningResult, int index) {
			if (index >= 0) {
				Object ret = scanningResult.getValues().get(index);
				return ret == null ? null : ret.toString();
			}
			return null;
		}
	}

	public enum Fetcher {
		Ping,
		Hostname,
		WebDetect,
		HTTPSender,
		NetBIOSInfo,
		PacketLoss,
		Ports,
		MACVendor,
		MAC,
		PingTTL,
		HttpProxy
	}
}
