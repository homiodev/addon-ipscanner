package org.homio.addon.ipscanner;

import static org.homio.addon.ipscanner.IPScanResultConsolePlugin.PLUGIN_NAME;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.IPScannerService.Fetcher;
import net.azib.ipscan.core.state.ScanningState;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStartButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStopButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleScanDeadHostsSetting;
import org.homio.addon.ipscanner.setting.ConsoleSelectedFetchersSetting;
import org.homio.api.AddonEntrypoint;
import org.homio.api.EntityContext;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class IPScannerBundleEntrypoint implements AddonEntrypoint {

  private final EntityContext entityContext;
  private IPScannerService ipScannerService;
  private CountDownLatch latch;
  private Set<Fetcher> scheduleFetch;

  @SneakyThrows
  @Override
  public void init() {
    ipScannerService = new IPScannerService(entityContext);
    ipScannerService.setCompleteHandler(() -> latch.countDown());

    entityContext.setting().listenValue(ConsoleScanDeadHostsSetting.class, "hosts", () ->
        entityContext.ui().console().refreshPluginContent(PLUGIN_NAME));
    entityContext.setting().listenValue(ConsoleHeaderStopButtonSetting.class, "stop", () -> {
      if (ipScannerService.getStateMachine().getState() == ScanningState.SCANNING) {
        entityContext.ui().dialog().sendConfirmation("ipscanner-stop-request", "W.CONFIRM.STOP_SCAN_TITLE",
            this::stopScanning, List.of("W.CONFIRM.STOP_SCAN_MESSAGE", "W.CONFIRM.STOP_SCAN_MESSAGE2"), null);
      }
    });
    entityContext.setting().listenValue(ConsoleHeaderStartButtonSetting.class, "start", this::startScanning);
    entityContext.setting().listenValueAndGet(ConsoleSelectedFetchersSetting.class, "fetchers", fetchers -> {
      if (ipScannerService.getStateMachine().getState() != ScanningState.IDLE) {
        this.scheduleFetch = fetchers;
        entityContext.ui().toastr().warn("W.ERROR.UPDATE_FETCH_DELAYED");
        return;
      }
      ipScannerService.getFetcherRegistry().setFetchers(fetchers);
    });

    IPScanResultConsolePlugin plugin = new IPScanResultConsolePlugin(entityContext, ipScannerService);
    entityContext.ui().console().registerPlugin(PLUGIN_NAME, plugin);
  }

  @Override
  public void destroy() {
    if (!ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      stopScanning();
    }
    entityContext.ui().console().unRegisterPlugin(PLUGIN_NAME);
  }

  private void startScanning(JSONObject jsonObject) {
    if (ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      entityContext.ui().console().refreshPluginContent(PLUGIN_NAME, List.of());
      latch = new CountDownLatch(1);
      entityContext.bgp().runWithProgress(PLUGIN_NAME).setLogToConsole(false).execute(progressBar -> {
        ipScannerService.startScan(
            jsonObject.getString("startIP"),
            jsonObject.getString("endIP"),
            jsonObject.getString("scanPorts"),
            progressBar);
        latch.await();
        if (scheduleFetch != null) {
          ipScannerService.getFetcherRegistry().setFetchers(scheduleFetch);
          scheduleFetch = null;
        }
      });
    } else {
      entityContext.ui().toastr().error("W.ERROR.IPSCANNER_NOT_FINISHED");
    }
  }

  private void stopScanning() {
    ipScannerService.getStateMachine().stop(); // stopping
    ipScannerService.getStateMachine().transitionToNext(); // killing
  }
}
















