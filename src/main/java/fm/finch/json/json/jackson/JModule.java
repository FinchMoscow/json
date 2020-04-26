package fm.finch.json.json.jackson;

import com.fasterxml.jackson.core.Version;


public class JModule extends com.fasterxml.jackson.databind.Module {
  @Override
  public String getModuleName() {
    return "JModule";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addSerializers(new JSerializers());
    context.addDeserializers(new JDeserializers());
  }
}
