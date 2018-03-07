package finch.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import finch.json.Json;
import finch.json.ToJson;

import java.io.IOException;

public class JSerializer extends com.fasterxml.jackson.databind.JsonSerializer<Object> {

  @Override
  public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    if (value instanceof Json) {
      gen.writeTree(((Json) value).jsonNode());
    } else if (value instanceof ToJson) {
      serialize(((ToJson) value).toJson(), gen, serializers);
    }
  }
}
