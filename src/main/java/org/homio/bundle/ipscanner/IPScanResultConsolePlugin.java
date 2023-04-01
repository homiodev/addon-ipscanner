package org.homio.bundle.ipscanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.core.ScanningResult;
import org.homio.bundle.ipscanner.setting.ConsoleShowDeadHostsSetting;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.console.ConsolePluginTable;
import org.homio.bundle.api.model.HasEntityIdentifier;
import org.homio.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.bundle.api.ui.field.UIField;
import org.homio.bundle.ipscanner.IPScanResultConsolePlugin.IpAddressPluginModel;
import org.homio.bundle.ipscanner.setting.IpScannerHeaderStartButtonSetting;

@Component
@RequiredArgsConstructor
public class IPScanResultConsolePlugin implements ConsolePluginTable<IpAddressPluginModel> {

  @Getter
  private final EntityContext entityContext;
  private final IPScannerService ipScannerService;

  @Override
  public int order() {
    return 2000;
  }

  @Override
  public Collection<IpAddressPluginModel> getValue() {
    List<IpAddressPluginModel> list = new ArrayList<>();
    Boolean showDeadHosts = entityContext.setting().getValue(ConsoleShowDeadHostsSetting.class);
    for (IPScannerService.ResultValue resultValue : ipScannerService.getIpScannerContext().getScanningResults()) {
      if (showDeadHosts || resultValue.type != ScanningResult.ResultType.DEAD) {
        list.add(new IpAddressPluginModel(resultValue));
      }
    }
    Collections.sort(list);

    return list;
  }

  @Override
  public Map<String, Class<? extends ConsoleHeaderSettingPlugin<?>>> getHeaderActions() {
    return Collections.singletonMap("ipscanner.start", IpScannerHeaderStartButtonSetting.class);
  }

  @Override
  public Class<IpAddressPluginModel> getEntityClass() {
    return IpAddressPluginModel.class;
  }

  @Getter
  @NoArgsConstructor
  public static class IpAddressPluginModel implements HasEntityIdentifier, Comparable<IpAddressPluginModel> {

    @UIField(order = 1)
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
    private ScanningResult.ResultType type;

    public IpAddressPluginModel(IPScannerService.ResultValue resultValue) {
      this.type = resultValue.type;
      this.ip = resultValue.ipFetcherValue;
      this.ping = resultValue.ping;
      this.hostname = resultValue.hostname;
      this.webDetectValue = resultValue.webDetectValue;
      this.httpSenderValue = resultValue.httpSenderValue;
      this.netBIOSInfo = resultValue.netBIOSInfo;
      this.ports = resultValue.ports;
      this.macVendorValue = resultValue.macVendorValue;
      this.macFetcherValue = resultValue.macFetcherValue;
    }

    @Override
    public String getEntityID() {
      return ip;
    }

    @Override
    public int compareTo(@NotNull IPScanResultConsolePlugin.IpAddressPluginModel o) {
      return this.ip.compareTo(o.ip);
    }
  }
}
