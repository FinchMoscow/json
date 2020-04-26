package fm.finch.json.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import fm.finch.json.json.Json;

import java.io.IOException;

public class JDeserialize extends JsonDeserializer<Json> {

  @Override
  public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return new Json(p.readValueAsTree());
  }

  @Override
  public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
    return new Json(p.readValueAsTree());
  }

}
