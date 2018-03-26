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
import com.google.common.collect.Streams;
import finch.json.jackson.JDeserialize;
import finch.json.jackson.JModule;
import finch.json.jackson.JSerializer;
import lombok.SneakyThrows;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonSerialize(using = JSerializer.class)
@JsonDeserialize(using = JDeserialize.class)
public class Json implements Iterable<Json> {
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JModule())
    .findAndRegisterModules()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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

  public static Json json(String key, Object o) {
    return object().set(key, o);
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
    return select((Object[]) path.split("\\."));
  }

  public Json select(Object... path) {
    Json j = this;
    for (Object p : path) {
      if (p instanceof Number) {
        j = j.get(((Number) p).intValue());
      } else {
        j = j.get((String) p);
      }
    }
    return j;
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

  public Json filterFields(BiFunction<String, Json, Boolean> fn) {
    return filterFields(fn, true);
  }

  public Json filterFields(BiFunction<String, Json, Boolean> fn, boolean recursive) {
    return map((k, v) -> fn.apply(k, v) ? v : null, recursive);
  }

  public Json filterFields(String fields) {
    Set<String> fieldSet = Stream.of(fields.split("\\s*,\\s*")).collect(Collectors.toSet());
    return filterFields((s, json) -> fieldSet.contains(s));
  }

  public Json map(BiFunction<String, Json, Object> fn) {
    return map(fn, true);
  }

  public Json map(BiFunction<String, Json, Object> fn, boolean recursive) {
    return map(fn, recursive, "");
  }

  public Json map(BiFunction<String, Json, Object> fn, boolean recursive, String path) {
    if (isArray()) {
      Json json = json();
      for (Json el : this) {
        if (!el.isNull()) {
          json.add(recursive ? el.map(fn, true, path) : fn.apply(path, el));
        }
      }
      return json;
    }
    if (isObject()) {
      Json json = json();
      for (String k : this.keySet()) {
        Json old = get(k);
        Json el = recursive ? old.map(fn, true, path.length() > 0 ? path + "." + k : k) : json(fn.apply(path, old));
        if (!el.isNull()) {
          json.set(k, el);
        }
      }
      return json;
    }
    return json(fn.apply(path, this));
  }

  public Json update(Json operations) {
    operations.fields().forEach(opField -> {
      if ("$set".equals(opField.getKey())) {
        opField.getValue()
          .fields()
          .forEach(setField ->
            select(setField.getKey())
              .set(setField.getValue())
          );
      }
    });

    return this;
  }

  public Stream<Map.Entry<String, Json>> fields() {
    return Streams.stream(jsonNode.fields()).map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), new Json(e.getValue(), Json.this, e.getKey())));
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
    return isArray() ? arrayNode().size() : 0;
  }

  public boolean isEmpty() {
    return isArray() ? arrayNode().size() == 0 : (isObject() ? objectNode().fields().hasNext() : (!isString() || asString().isEmpty()));
  }


  private ArrayNode arrayNode() {
    return (ArrayNode) jsonNode;
  }

  private ObjectNode objectNode() {
    return (ObjectNode) jsonNode;
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
    if (parameterClasses.length == 0) {
      return (T) OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(jsonNode), type);
    }
    return OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(jsonNode), OBJECT_MAPPER.getTypeFactory().constructParametricType(type, parameterClasses));
  }

  public Number asNumber(Number value) {
    return isNumber() ? as(Number.class) : (isString() ? parseDouble(asString(), value) : null);
  }


  public int asInt() {
    return asInt(0);
  }

  public int asInt(int value) {
    return isNumber() ? as(Integer.class) : (isString() ? parseInt(asString(), value) : value);
  }


  public long asLong(long value) {
    return isNumber() ? as(Long.class) : (isString() ? parseLong(asString(), value) : value);
  }


  public String asString() {
    return asString("");
  }

  public String asString(String value) {
    if(isNumber()) {
      return asNumber(0).toString();
    }
    return isString() ? as(String.class) : value;
  }

  public boolean asBoolean() {
    return asBoolean(false);
  }

  public boolean asBoolean(boolean value) {
    return isBoolean() ? as(Boolean.class) : value;
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

  private int parseInt(String s, int value) {
    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value;
    }
  }

  private long parseLong(String s, long value) {
    try {
      return Long.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value;
    }
  }

  private double parseDouble(String s, Number value) {
    try {
      return Double.valueOf(s);
    } catch (NumberFormatException ignored) {
      return value.doubleValue();
    }
  }


}
