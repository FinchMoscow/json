package fm.finch.json.json;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fm.finch.json.json.jackson.JSerializer;

@JsonSerialize(using = JSerializer.class)
public interface ToJson {

  Json toJson();

}
