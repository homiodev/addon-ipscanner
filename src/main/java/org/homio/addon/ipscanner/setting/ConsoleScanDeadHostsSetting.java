package org.homio.addon.ipscanner.setting;

import static org.homio.addon.ipscanner.IPScanResultConsolePlugin.PLUGIN_NAME;

import org.homio.api.setting.SettingPluginBoolean;
import org.homio.api.setting.console.ConsoleSettingPlugin;

public class ConsoleScanDeadHostsSetting implements ConsoleSettingPlugin<Boolean>, SettingPluginBoolean {

  @Override
  public int order() {
    return 100;
  }

  @Override
  public String[] pages() {
    return new String[]{PLUGIN_NAME};
  }
}
