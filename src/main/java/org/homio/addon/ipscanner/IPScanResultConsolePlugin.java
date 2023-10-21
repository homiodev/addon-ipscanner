package org.homio.addon.ipscanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningResult;
import org.homio.addon.ipscanner.IPScanResultConsolePlugin.IpAddressPluginModel;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStartButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleHeaderStopButtonSetting;
import org.homio.addon.ipscanner.setting.ConsoleSelectedFetchersSetting;
import org.homio.api.Context;
import org.homio.api.console.ConsolePluginTable;
import org.homio.api.model.HasEntityIdentifier;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.color.UIFieldColorBgRef;

@RequiredArgsConstructor
public class IPScanResultConsolePlugin implements ConsolePluginTable<IpAddressPluginModel> {

  public static final String PLUGIN_NAME = "IpScanner";

  private final @Getter @Accessors(fluent = true) Context context;
  private final IPScannerService ipScannerService;

  @Override
  public int order() {
    return 2000;
  }

  @Override
  public boolean hasRefreshIntervalSetting() {
    return false;
  }

  @Override
  public Collection<IpAddressPluginModel> getValue() {
    List<IpAddressPluginModel> list = new ArrayList<>();
    boolean showDeadHosts = ipScannerService.getScannerConfig().scanDeadHosts;
    for (IPScannerService.ResultValue resultValue : ipScannerService.getIpScannerContext().getScanningResults()) {
      if (showDeadHosts || resultValue.type != ScanningResult.ResultType.DEAD) {
        list.add(new IpAddressPluginModel(resultValue));
      }
    }

    return list;
  }

  @Override
  public Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> getHeaderActions() {
    return Map.of(
        "start", ConsoleHeaderStartButtonSetting.class,
        "stop", ConsoleHeaderStopButtonSetting.class,
        "fetchers", ConsoleSelectedFetchersSetting.class
    );
  }

  @Override
  public Class<IpAddressPluginModel> getEntityClass() {
    return IpAddressPluginModel.class;
  }

  @Getter
  @NoArgsConstructor
  public static class IpAddressPluginModel implements HasEntityIdentifier {

    @UIField(order = 1)
    @UIFieldColorBgRef("color")
    private String ip;

    @UIField(order = 2)
    private String hostname;

    @UIField(order = 3)
    private String ping;

    @UIField(order = 5)
    private String webDetectValue;

    @UIField(order = 6)
    private String httpSenderValue;

    @UIField(order = 9)
    private String netBIOSInfo;

    @UIField(order = 11)
    private String ports;

    @UIField(order = 12)
    private String macVendorValue;

    @UIField(order = 14)
    private String macFetcherValue;

    @UIField(order = 15)
    private ScanningResult.ResultType resultType;

    private String color;

    public IpAddressPluginModel(IPScannerService.ResultValue resultValue) {
      this.resultType = resultValue.type;
      this.ip = resultValue.address;
      this.ping = resultValue.ping;
      this.hostname = resultValue.hostname;
      this.webDetectValue = resultValue.webDetectValue;
      this.httpSenderValue = resultValue.httpSenderValue;
      this.netBIOSInfo = resultValue.netBIOSInfo;
      this.ports = resultValue.ports;
      this.macVendorValue = resultValue.macVendorValue;
      this.macFetcherValue = resultValue.macFetcherValue;
      this.color = resultType.getColor();
    }

    @Override
    public String getEntityID() {
      return ip;
    }
  }
}
