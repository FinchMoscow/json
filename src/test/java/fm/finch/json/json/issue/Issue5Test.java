package fm.finch.json.json.issue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import fm.finch.json.json.Json;
import org.junit.Test;

import java.io.IOException;

public class Issue5Test {

  @Test
  public void issue5() throws IOException {
    String jsonString = Json.json().set("key", "value").toString();
    ObjectMapper mapper = new ObjectMapper()
      .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    mapper.readValue(jsonString, Json.class);
  }
}
