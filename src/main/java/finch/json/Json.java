package finch.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import finch.json.jackson.JDeserialize;
import finch.json.jackson.JModule;
import finch.json.jackson.JSerializer;
import lombok.SneakyThrows;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@JsonSerialize(using = JSerializer.class)
@JsonDeserialize(using = JDeserialize.class)
public class Json implements Iterable<Json> {
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JModule())
    .findAndRegisterModules()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
  private final Json parent;
  private final Object path;

  private JsonNode jsonNode;

  public Json(JsonNode jsonNode) {
    this(jsonNode, null, null);
  }

  Json(JsonNode jsonNode, Json parent, Object path) {
    if (jsonNode == null) {
      jsonNode = NullNode.getInstance();
    }
    this.jsonNode = jsonNode;
    this.parent = parent;
    this.path = path;
  }

  public static Json json() {
    return json(null);
  }

  public static Json json(Object o) {
    return new Json(OBJECT_MAPPER.valueToTree(o));
  }

  public static Json object() {
    return json(Collections.emptyMap());
  }

  public static Json array() {
    return json(Collections.emptyList());
  }

  @SneakyThrows
  public static Json parse(String json) {
    return new Json(OBJECT_MAPPER.readTree(json));
  }

  public Json clone() {
    return json(jsonNode.deepCopy());
  }

  public JsonNode jsonNode() {
    return jsonNode;
  }

  public boolean isString() {
    return jsonNode.isTextual();
  }

  public boolean isObject() {
    return jsonNode.isObject();
  }

  public boolean isArray() {
    return jsonNode.isArray();
  }

  public boolean isNumber() {
    return jsonNode.isNumber();
  }

  public boolean isNull() {
    return jsonNode.isNull();
  }

  public boolean isBoolean() {
    return jsonNode.isBoolean();
  }

  public Json select(String path) {
    return selectByPath(Arrays.asList(path.split("\\.")));
  }

  private Json selectByPath(Iterable<String> path) {
    return selectByPath(path.iterator());
  }

  private Json selectByPath(Iterator<String> path) {
    if (!path.hasNext())
      return this;
    String next = path.next();
    if (next.charAt(0) >= '1' && next.charAt(0) <= '0') {
      int i = Integer.valueOf(next);
      return get(i).selectByPath(path);
    }
    return get(next).selectByPath(path);
  }

  public Json get(int i) {
    JsonNode nextElement = NullNode.getInstance();
    if (isArray() && arrayNode().size() > 0)
      nextElement = arrayNode().get(i);
    return new Json(nextElement, this, i);
  }

  public Json get(String prop) {
    JsonNode nextElement = NullNode.getInstance();
    if (isObject() && objectNode().has(prop))
      nextElement = objectNode().get(prop);
    return new Json(nextElement, this, prop);
  }

  public Json set(Object value) {
    updateElement(valueToTree(value));
    return this;
  }

  public Json set(int i, Object value) {
    if (!isArray()) {
      updateElement(OBJECT_MAPPER.createArrayNode());
    }
    while (arrayNode().size() <= i - 1) {
      arrayNode().add(NullNode.getInstance());
    }
    if (arrayNode().size() >= i + 1) {
      ArrayNode newArray = OBJECT_MAPPER.createArrayNode();
      ArrayNode oldArray = arrayNode();
      for (int j = 0; j < oldArray.size(); j++) {
        if (j == i)
          newArray.add(valueToTree(value));
        else
          newArray.add(oldArray.get(j));
      }
      updateElement(newArray);
    } else {
      arrayNode().add(valueToTree(value));
    }
    return this;
  }


  public Json set(String prop, Object value) {
    if (!isObject()) {
      updateElement(OBJECT_MAPPER.createObjectNode());
    }
    objectNode().set(prop, valueToTree(value));
    return this;
  }


  public Json add(Object value) {
    if (isArray())
      set(arrayNode().size(), value);
    else
      set(0, value);
    return this;
  }

  private void updateElement(JsonNode newJsonNode) {
    if (parent != null) {
      if (path instanceof String) {
        parent.set((String) path, newJsonNode);
        newJsonNode = parent.get((String) path).jsonNode;
      } else {
        parent.set((Integer) path, newJsonNode);
        newJsonNode = parent.get((Integer) path).jsonNode;
      }
    }
    this.jsonNode = newJsonNode;
  }

  private JsonNode valueToTree(Object value) {
    if (value == null) {
      return NullNode.getInstance();
    }
    if (value instanceof Json) {
      return ((Json) value).jsonNode();
    }
    return OBJECT_MAPPER.valueToTree(value);
  }

  @Override
  public Iterator<Json> iterator() {
    if (isArray()) {
      return new Iterator<Json>() {
        private int i = 0;

        @Override
        public boolean hasNext() {
          return i < arrayNode().size();
        }

        @Override
        public Json next() {
          return get(i++);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
    return Collections.<Json>emptyList().iterator();
  }

  public Set<String> keySet() {
    if (isObject()) {
      HashSet<String> keys = new HashSet<>();
      objectNode().fieldNames().forEachRemaining(keys::add);
      return keys;
    }
    return Collections.emptySet();
  }

  public int size() {
    return arrayNode().size();
  }

  public boolean isEmpty() {
    return isArray() ? arrayNode().size() == 0 : (isObject() ? keySet().size() == 0 : (!isString() || asString().isEmpty()));
  }


  private ArrayNode arrayNode() {
    return (ArrayNode) jsonNode;
  }

  private ObjectNode objectNode() {
    return (ObjectNode) jsonNode;
  }

  public Json filter(BiFunction<String, Json, Boolean> fn) {
    return map((k, v) -> fn.apply(k, v) ? v : null, "");
  }

  public Json map(BiFunction<String, Json, Object> fn) {
    return map(fn, "");
  }

  public Json map(BiFunction<String, Json, Object> fn, String path) {
    if (isArray()) {
      Json json = json();
      for (Json el : this) {
        if (!el.isNull()) {
          json.add(el.map(fn, path));
        }
      }
      return json;
    }
    if (isObject()) {
      Json json = json();
      for (String k : this.keySet()) {
        Json old = get(k);
        Json el = old.map(fn, path.length() > 0 ? path + "." + k : k);
        if (!el.isNull()) {
          json.set(k, el);
        }
      }
      return json;
    }
    return json(fn.apply(path, this));
  }

  public Json mapValues(Function<Json, Object> fn, boolean recursive) {
    if (isArray()) {
      Json json = array();
      for (Json el : this) {
        json.add(recursive ? el.mapValues(fn, true) : fn.apply(el));
      }
      return json;
    }
    if (isObject()) {
      Json json = object();
      for (String k : this.keySet()) {
        Json el = get(k);
        json.set(k, recursive ? el.mapValues(fn, true) : fn.apply(el));
      }
      return json;
    }
    return json(fn.apply(this));
  }

  public Json mapValues(Function<Json, Boolean> filter, Function<Json, Object> fn, boolean recursive) {
    return mapValues(json -> filter.apply(json) ? fn.apply(json) : json, recursive);
  }

  @Override
  public String toString() {
    return jsonNode.toString();
  }

  @SneakyThrows
  public String toPretty() {
    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
  }

  @SneakyThrows
  public <T> T as(Class<? super T> type, Class... parameterClasses) {
    if (type.isAssignableFrom(Json.class)) {
      return (T) this;
    }
    return OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(jsonNode), OBJECT_MAPPER.getTypeFactory().constructParametricType(type, parameterClasses));
  }

  @SneakyThrows
  public <T> T as(Class<T> type) {
    if (type.isAssignableFrom(Json.class)) {
      return (T) this;
    }
    return OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(jsonNode), type);
  }

  public Number asNumber(Number value) {
    return isNumber() ? as(Number.class) : (isString() ? parseDouble(asString(), value) : null);
  }

  private double parseDouble(String s, Number value) {
    try {
      return Double.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value.doubleValue();
    }
  }

  public int asInt() {
    return asInt(0);
  }

  public int asInt(int value) {
    return isNumber() ? as(Integer.class) : (isString() ? parseInt(asString(), value) : value);
  }

  private int parseInt(String s, int value) {
    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value;
    }
  }

  public long asLong(long value) {
    return isNumber() ? as(Long.class) : (isString() ? parseLong(asString(), value) : value);
  }

  private long parseLong(String s, long value) {
    try {
      return Long.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value;
    }
  }

  public String asString() {
    return asString("");
  }

  public String asString(String value) {
    return isString() ? as(String.class) : value;
  }

  public boolean asBoolean() {
    return asBoolean(false);
  }

  public boolean asBoolean(boolean value) {
    return isBoolean() ? as(Boolean.class) : value;
  }
}
