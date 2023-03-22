package org.touchhome.bundle.ipscanner.setting;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.console.header.ConsoleHeaderSettingPlugin;
import org.touchhome.bundle.api.ui.field.action.ActionInputParameter;

public class IpScannerHeaderStartButtonSetting implements ConsoleHeaderSettingPlugin<JSONObject>, SettingPluginButton {

  @Override
  public String getIcon() {
    return "fas fa-search-location";
  }

  @Override
  public int order() {
    return 100;
  }

  @Override
  public List<ActionInputParameter> getInputParameters(EntityContext entityContext, String value) {
    return Arrays.asList(
        ActionInputParameter.ip("startIP", "0.0.0.0"),
        ActionInputParameter.ip("endIP", "0.0.0.1")
    );
  }
}
