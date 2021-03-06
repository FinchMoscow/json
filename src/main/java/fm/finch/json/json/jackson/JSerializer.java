package fm.finch.json.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import fm.finch.json.json.Json;
import fm.finch.json.json.ToJson;

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

  @Override
  public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
    if (value instanceof Json) {
      gen.writeTree(((Json) value).jsonNode());
    } else if (value instanceof ToJson) {
      serialize(((ToJson) value).toJson(), gen, serializers);
    } else {
      super.serializeWithType(value, gen, serializers, typeSer);
    }
  }
}
