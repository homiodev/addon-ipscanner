package org.touchhome.bundle.ipscanner.setting;

import org.touchhome.bundle.api.setting.SettingPluginBoolean;
import org.touchhome.bundle.api.setting.console.ConsoleSettingPlugin;

public class ConsoleShowDeadHostsSetting implements ConsoleSettingPlugin<Boolean>, SettingPluginBoolean {

  @Override
  public int order() {
    return 100;
  }

  @Override
  public String[] pages() {
    return new String[]{"ipscanner"};
  }

  @Override
  public boolean defaultValue() {
    return true;
  }
}
