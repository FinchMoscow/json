package finch.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import finch.json.Json;

import java.io.IOException;

public class JDeserialize extends JsonDeserializer<Json> {
  @Override
  public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    return new Json(p.readValueAsTree());
  }
}
