package org.homio.addon.ipscanner.setting;

import java.util.Arrays;
import java.util.List;
import org.homio.api.Context;
import org.homio.api.model.Icon;
import org.homio.api.setting.SettingPluginButton;
import org.homio.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.homio.api.ui.field.action.ActionInputParameter;
import org.homio.api.util.HardwareUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

public class ConsoleHeaderStartButtonSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public @Nullable String getConfirmMsg() {
    return null;
  }

  @Override
  public Icon getIcon() {
    return new Icon("fas fa-search-location");
  }

  @Override
  public int order() {
    return 10;
  }

  @Override
  public List<ActionInputParameter> getInputParameters(Context context, String value) {
    String prefix = HardwareUtils.MACHINE_IP_ADDRESS.substring(0, HardwareUtils.MACHINE_IP_ADDRESS.lastIndexOf(".")) + ".";
    return Arrays.asList(
        ActionInputParameter.ip("startIP", prefix + "0"),
        ActionInputParameter.ip("endIP", prefix + "255"),
        ActionInputParameter.text("scanPorts", "80,443,8080-8084")
    );
  }
}
