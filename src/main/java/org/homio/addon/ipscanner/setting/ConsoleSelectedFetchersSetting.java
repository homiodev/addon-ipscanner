package org.homio.addon.ipscanner.setting;

import java.util.Set;
import net.azib.ipscan.IPScannerService;
import net.azib.ipscan.IPScannerService.Fetcher;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginOptionsEnumMulti;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.jetbrains.annotations.Nullable;

public class ConsoleSelectedFetchersSetting implements ConsoleHeaderSettingPlugin<Set<Fetcher>>, SettingPluginOptionsEnumMulti<IPScannerService.Fetcher> {

  @Override
  public IPScannerService.Fetcher[] defaultValue() {
    return new IPScannerService.Fetcher[]{Fetcher.Ping, Fetcher.Hostname, Fetcher.Ports};
  }

  @Override
  public Class<IPScannerService.Fetcher> getEnumType() {
    return IPScannerService.Fetcher.class;
  }

  @Override
  public boolean viewAsButton() {
    return true;
  }

  @Override
  public @Nullable Icon getIcon() {
    return new Icon("fas fa-square-rss");
  }

  @Override
  public int order() {
    return 50;
  }
}
