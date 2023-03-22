package org.touchhome.bundle.ipscanner.setting;

import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.setting.SettingPluginButton;
import org.touchhome.bundle.api.setting.console.ConsoleSettingPlugin;
import org.touchhome.bundle.api.ui.field.action.ActionInputParameter;

public class ConsoleScannedPortsSetting implements ConsoleSettingPlugin<JSONObject>, SettingPluginButton {

  private static final String DEFAULT_VALUE = new JSONObject().put("ipscanner_ports", "80,443,8080").toString();

  @Override
  public String getIcon() {
    return "fab fa-megaport";
  }

  @Override
  public int order() {
    return 200;
  }

  @Override
  public String[] pages() {
    return new String[]{"ipscanner"};
  }

  @Override
  public String getDefaultValue() {
    return DEFAULT_VALUE;
  }

  @Override
  public List<ActionInputParameter> getInputParameters(EntityContext entityContext, String value) {
    return Collections.singletonList(ActionInputParameter.textarea("ipscanner_ports", getDefaultValue())
        .setDescription("ipscanner_ports_description"));
  }

  @Override
  public boolean isReverted() {
    return true;
  }
}
