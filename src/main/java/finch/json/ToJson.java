package finch.json;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import finch.json.jackson.JSerializer;

@JsonSerialize(using = JSerializer.class)
public interface ToJson {
  Json toJson();
}
