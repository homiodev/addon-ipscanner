package org.homio.addon.ipscanner;

import static org.homio.addon.ipscanner.IPScanResultConsolePlugin.PLUGIN_NAME;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.IPScannerService.Fetcher;
import net.azib.ipscan.core.state.ScanningState;
import net.azib.ipscan.core.state.StateMachine;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStartButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStopButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleScanDeadHostsSetting;
import org.homio.addon.ipscanner.setting.ConsoleSelectedFetchersSetting;
import org.homio.api.AddonEntrypoint;
import org.homio.api.Context;
import org.homio.api.util.Lang;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class IPScannerBundleEntrypoint implements AddonEntrypoint {

  private final Context context;
  private IPScannerService ipScannerService;
  private CountDownLatch latch;
  private Set<Fetcher> scheduleFetch;

  @SneakyThrows
  @Override
  public void init() {
    ipScannerService = new IPScannerService(context);
    ipScannerService.setCompleteHandler(() -> latch.countDown());

    context.setting().listenValue(ConsoleScanDeadHostsSetting.class, "hosts", () ->
        context.ui().console().refreshPluginContent(PLUGIN_NAME));
    context.setting().listenValue(ConsoleHeaderStopButtonSetting.class, "stop", () -> {
      ScanningState state = ipScannerService.getStateMachine().getState();
      if (state == ScanningState.SCANNING || state == ScanningState.STOPPING) {
        context.ui().dialog().sendConfirmation("ipscanner-stop-request", "W.CONFIRM.STOP_SCAN_TITLE",
            this::stopScanning, List.of("W.CONFIRM.STOP_SCAN_MESSAGE", "W.CONFIRM.STOP_SCAN_MESSAGE2"), null);
      } else {
        context.ui().toastr().warn(Lang.getServerMessage("IP_SCANNER_UNABLE_STOP", state.toString()));
      }
    });
    context.setting().listenValue(ConsoleHeaderStartButtonSetting.class, "start", this::startScanning);
    context.setting().listenValueAndGet(ConsoleSelectedFetchersSetting.class, "fetchers", fetchers -> {
      if (ipScannerService.getStateMachine().getState() != ScanningState.IDLE) {
        this.scheduleFetch = fetchers;
        context.ui().toastr().warn("W.ERROR.UPDATE_FETCH_DELAYED");
        return;
      }
      ipScannerService.getFetcherRegistry().setFetchers(fetchers);
    });

    IPScanResultConsolePlugin plugin = new IPScanResultConsolePlugin(context, ipScannerService);
    context.ui().console().registerPlugin(PLUGIN_NAME, plugin);
  }

  @Override
  public void destroy() {
    if (!ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      stopScanning();
    }
    context.ui().console().unRegisterPlugin(PLUGIN_NAME);
  }

  private void startScanning(JSONObject jsonObject) {
    if (ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      // send empty array list
      context.ui().console().refreshPluginContent(PLUGIN_NAME, List.of());
      latch = new CountDownLatch(1);
      context.bgp().runWithProgress(PLUGIN_NAME).setLogToConsole(false)
                   .onFinally(() -> {
                     context.ui().console().refreshPluginContent(PLUGIN_NAME);
                     if (scheduleFetch != null) {
                       ipScannerService.getFetcherRegistry().setFetchers(scheduleFetch);
                       scheduleFetch = null;
                     }
                   })
                   .execute(progressBar -> {
                     ipScannerService.startScan(
                         jsonObject.getString("startIP"),
                         jsonObject.getString("endIP"),
                         jsonObject.getString("scanPorts"),
                         progressBar);
                     latch.await();
                   });
    } else {
      context.ui().toastr().error("W.ERROR.IPSCANNER_NOT_FINISHED");
    }
  }

  private void stopScanning() {
    StateMachine state = ipScannerService.getStateMachine();
    if (state.inState(ScanningState.SCANNING)) {
      state.transitionToNext(); // STOPPING
      context.bgp().builder("ipscanner-kill").delay(Duration.ofSeconds(10))
                   .execute(() -> {
                     if (state.inState(ScanningState.STOPPING)) {
                       state.transitionToNext();
                     }
                   });
    }
  }
}
















