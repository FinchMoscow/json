package finch.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class BagTests {
  @Test
  public void issue4() throws JsonProcessingException {
    Json anyJson = Json.json();
    ObjectMapper mapper = new ObjectMapper()
      .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    mapper.writeValueAsString(anyJson);
  }

  @Test
  public void issue5() throws IOException {
    String jsonString = Json.json().set("key", "value").toString();
    ObjectMapper mapper = new ObjectMapper()
      .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    mapper.readValue(jsonString, Json.class);
  }
}
