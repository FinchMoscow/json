package fm.finch.json.json.issue;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fm.finch.json.json.Json;
import org.junit.Test;

public class Issue4Test {

  @Test
  public void issue4() throws JsonProcessingException {
    Json anyJson = Json.json();
    ObjectMapper mapper = new ObjectMapper()
      .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    mapper.writeValueAsString(anyJson);
  }

}
