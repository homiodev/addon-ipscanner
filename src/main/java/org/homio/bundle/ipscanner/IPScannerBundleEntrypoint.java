package org.homio.bundle.ipscanner;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.state.ScanningState;
import org.homio.bundle.ipscanner.setting.ConsoleScannedPortsSetting;
import org.homio.bundle.ipscanner.setting.IpScannerHeaderStartButtonSetting;
import org.homio.bundle.ipscanner.setting.IpScannerHeaderStopButtonSetting;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.BundleEntrypoint;
import org.homio.bundle.api.EntityContext;

@Log4j2
@Component
@RequiredArgsConstructor
public class IPScannerBundleEntrypoint implements BundleEntrypoint {

  private final EntityContext entityContext;
  private final IPScannerService ipScannerService;

  @Override
  public void init() {
    String scanEntityID = "ip-scanner-scan";
    ipScannerService.getScannerConfig().portString =
        entityContext.setting().getValue(ConsoleScannedPortsSetting.class).getString("ipscanner_ports");
    ipScannerService.setCompleteHandler(() -> entityContext.ui().removeHeaderButton(scanEntityID));

    entityContext.setting().listenValue(ConsoleScannedPortsSetting.class, "ipscanner-ports", jsonObject ->
        ipScannerService.getScannerConfig().portString = jsonObject.getString("ipscanner_ports"));
    entityContext.setting().listenValue(IpScannerHeaderStopButtonSetting.class, "ipscanner-stop", this::stopScanning);
    entityContext.setting().listenValue(IpScannerHeaderStartButtonSetting.class, "ipscanner-start",
        jsonObject -> startScanning(scanEntityID, jsonObject));
  }

  @Override
  public void destroy() {
    if (!ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      stopScanning();
    }
  }

  @Override
  public int order() {
    return 6000;
  }

  private void startScanning(String scanEntityID, JSONObject jsonObject) {
    if (ipScannerService.getStateMachine().inState(ScanningState.IDLE)) {
      entityContext.ui().headerButtonBuilder(scanEntityID).title("ipscanner.start_scan")
          .border(1, "#325E32").icon("fas fa-hourglass-start", null, true)
          .clickAction(IpScannerHeaderStopButtonSetting.class).build();
      ipScannerService.startScan(jsonObject.getString("startIP"), jsonObject.getString("endIP"));
    }
  }

  private void stopScanning() {
    ipScannerService.getStateMachine().stop(); // stopping
    ipScannerService.getStateMachine().transitionToNext(); // killing
  }
}
















