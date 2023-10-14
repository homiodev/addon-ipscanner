package org.homio.addon.ipscanner.setting;

import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginButton;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class ConsoleHeaderStopButtonSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton  {

  @Override
  public int order() {
    return 20;
  }

  @Override
  public @Nullable String getConfirmMsg() {
    return null;
  }

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-stop-circle");
  }
}
