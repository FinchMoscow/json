package finch.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class BagTests {
  @Test
  public void issue4() {
    Json anyJson = Json.json();
    ObjectMapper mapper = new ObjectMapper()
      .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    try {
      mapper.writeValueAsString(anyJson);
    } catch (JsonProcessingException expectedError) {
      expectedError.printStackTrace();
    }

  }
}
