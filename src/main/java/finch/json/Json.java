package finch.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import finch.json.jackson.JDeserialize;
import finch.json.jackson.JModule;
import finch.json.jackson.JSerializer;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonSerialize(using = JSerializer.class)
@JsonDeserialize(using = JDeserialize.class)
public class Json implements Iterable<Json> {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JModule())
    .findAndRegisterModules()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
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
    return missing();
  }

  public static Json missing() {
    return new Json(MissingNode.getInstance());
  }

  public static Json json(Object o) {
    if (o instanceof Json) {
      return (Json) o;
    }
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

  public boolean isMissing() {
    return jsonNode.isMissingNode();
  }

  public JsonCheck check(Function<Json, Object> fn) {
    return new JsonCheck(this, fn);
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
    JsonNode nextElement = MissingNode.getInstance();
    if (isArray() && arrayNode().size() >= i) {
      nextElement = arrayNode().get(i);
    }
    return new Json(nextElement, this, i);
  }

  public Json get(String prop) {
    JsonNode nextElement = MissingNode.getInstance();
    if (isObject() && objectNode().has(prop)) {
      nextElement = objectNode().get(prop);
    }
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
    JsonNode value1 = valueToTree(value);
    if (value1.isMissingNode()) {
      objectNode().remove(prop);
    } else {
      objectNode().set(prop, value1);
    }
    return this;
  }


  public Json add(Object value) {
    if (isArray())
      set(arrayNode().size(), value);
    else
      set(0, value);
    return this;
  }

  public Json filterFields(Class type) {
    return json(as(type));
  }

  public Json filterFields(BiFunction<String, Json, Boolean> fn) {
    return filterFields(fn, true);
  }

  public Json filterFields(BiFunction<String, Json, Boolean> fn, boolean recursive) {
    return map((k, v) -> fn.apply(k, v) ? v : missing(), recursive);
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
//        if (!el.isNull()) {
        Json mapped = recursive ? el.map(fn, true, path) : json(fn.apply(path, el));
        if (!mapped.isMissing()) {
          json.add(mapped);
        }
//        }
      }
      return json;
    }
    if (isObject()) {
      Json json = json();
      for (String k : this.keySet()) {
        Json old = get(k);
        Json el = recursive ? old.map(fn, true, path.length() > 0 ? path + "." + k : k) : json(fn.apply(path, old));
        if (!el.isMissing()) {
          json.set(k, el);
        }
      }
      return json;
    }
    return json(fn.apply(path, this));
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
    return isArray() ? arrayNode().size() == 0 : (isObject() ? !objectNode().fields().hasNext() : (!isString() || asString().isEmpty()));
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
  public <T> T to(T to) {
    OBJECT_MAPPER.readerForUpdating(to).readValue(jsonNode);
    return to;
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
    return isNumber() ? as(Number.class) : (isString() ? parseDouble(asString(), value) : value);
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
    if (isNumber()) {
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

  public Json removeNulls() {
    return filterFields((s, json) -> !json.isNull());
  }

  public Json toCamelCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, true, true, false, ""));
  }

  public Json toLowerCamelCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, false, true, false, ""));
  }

  public Json toSnakeCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, false, false, false, "_"));
  }

  public Json toKebabCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, false, false, false, "-"));
  }

  public Json toTrainCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, false, false, false, "-"));
  }

  public Json toScreamingSnakeCase() {
    return mapFieldNames(s -> changeFieldNameCase(s, false, false, true, "_"));
  }

  private String changeFieldNameCase(String fieldName, boolean capitalize, boolean camel, boolean upper, String join) {
    fieldName = Stream.of(fieldName.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[-_]"))
      .map(s -> {
        if (camel) {
          return StringUtils.capitalize(s);
        }
        return s.toLowerCase();
      })
      .collect(Collectors.joining(join));
    if (capitalize) {
      fieldName = StringUtils.capitalize(fieldName);
    } else {
      fieldName = StringUtils.uncapitalize(fieldName);
    }
    if (upper) {
      fieldName = fieldName.toUpperCase();
    }
    return fieldName;

  }

  public Json mapFieldNames(Function<String, String> fn) {
    if (isArray()) {
      Json json = json();
      for (Json el : this) {
        Json mapped = el.mapFieldNames(fn);
        if (!mapped.isMissing()) {
          json.add(mapped);
        }
      }
      return json;
    }
    if (isObject()) {
      Json json = json();
      for (String k : this.keySet()) {
        Json old = get(k);
        Json el = old.mapFieldNames(fn);
        if (!el.isMissing()) {
          json.set(fn.apply(k), el);
        }
      }
      return json;
    }
    return this;
  }

  public Json op(Json op) {
    if (isMissing() || isNull()) {
      op.get("$setOnInsert").fields().forEach(e -> select(e.getKey()).set(e.getValue()));
    }
    op.get("$set").fields().forEach(e -> select(e.getKey()).set(e.getValue()));
    op.get("$inc").fields().forEach(e -> {
      Json select = select(e.getKey());
      select.set(select.asNumber(0).doubleValue() + e.getValue().asNumber(0).doubleValue());
    });
    op.get("$min").fields().forEach(e -> {
      Json select = select(e.getKey());
      double old = select.asNumber(0).doubleValue();
      double value = e.getValue().asNumber(0).doubleValue();
      if (value < old) {
        select.set(value);
      }
    });
    op.get("$max").fields().forEach(e -> {
      Json select = select(e.getKey());
      double old = select.asNumber(0).doubleValue();
      double value = e.getValue().asNumber(0).doubleValue();
      if (value > old) {
        select.set(value);
      }
    });
    op.get("$mul").fields().forEach(e -> {
      Json select = select(e.getKey());
      select.set(select.asNumber(0).doubleValue() * e.getValue().asNumber(0).doubleValue());
    });
    op.get("$rename").fields().forEach(e -> {
      select(e.getValue().asString()).set(select(e.getKey()));
      select(e.getKey()).set(missing());
    });
    op.get("$unset").fields().forEach(e -> select(e.getKey()).set(missing()));
    op.get("$push").fields().forEach(o ->
      Optional.of(select(o.getKey())).filter(Json::isArray).ifPresent(j -> j.add(o.getValue()))
    );
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Json json = (Json) o;
    return Objects.equals(jsonNode, json.jsonNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonNode);
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
    if (value instanceof JsonNode) {
      return (JsonNode) value;
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
