package finch.json;

import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static finch.json.Json.json;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonTest {

  @Test
  public void removeNulls() {
    Json json = Json.json()
      .set("a", null)
      .set("b", Json.json()
        .set("a", null)
      )
      .set("c", Json.json()
        .set("a", null)
        .set("b", 1)
      );
    Json removeNulls = json().removeNulls();
    assertTrue(json.select("b").isObject());
    assertTrue(json.select("b.a").isNull());
    assertTrue(json.select("c.a").isNull());
    assertTrue(removeNulls.select("b").isMissing());
    assertTrue(removeNulls.select("c.a").isMissing());
  }

  @Test
  public void equalsAndHash() {
    Json j1 = Json.parse("{\"a\":1, \"b\":{\"c\":null}}");
    Json j2 = Json.parse("{\"b\":{\"c\":null}, \"a\":1}");
    Json j3 = Json.parse("{\"b\":{\"c\":null, \"d\":null}, \"a\":1}");
    assertTrue(j1.equals(j2));
    assertTrue(j1.hashCode() == j2.hashCode());
    assertFalse(j1.equals(j3));
    assertFalse(j1.hashCode() == j3.hashCode());
  }

  @Test
  public void changeFieldNameCase() {
    Json j = json("superWRMLTest-aaa", 1);
    assertTrue(j.get("superWRMLTest-aaa").isNumber());
    assertTrue(j.toCamelCase().get("SuperWRMLTestAaa").isNumber());
    assertTrue(j.toKebabCase().get("super-wrml-test-aaa").isNumber());
    assertTrue(j.toLowerCamelCase().get("superWRMLTestAaa").isNumber());
    assertTrue(j.toScreamingSnakeCase().get("SUPER_WRML_TEST_AAA").isNumber());
    assertTrue(j.toSnakeCase().get("super_wrml_test_aaa").isNumber());
    assertTrue(j.toTrainCase().get("super-wrml-test-aaa").isNumber());
  }

  @Test
  public void fixStructure() {
    Json json = json()
      .set("a", 1)
      .set("b", 2);
    json.get("a").get(2).set(3, 1);
    assertTrue(json.get("a").isArray());
    assertTrue(json.get("a").get(1).isNull());
    assertTrue(json.get("a").get(2).isArray());
    assertEquals(1, json.get("a").get(2).get(3).asInt());
    assertEquals(2, json.get("b").asInt());
  }

  @Test
  public void filter() {
    Json json = json()
      .set("a", 1)
      .set("b", json()
        .add(json()
          .set("a", 1)
          .set("b", 1)
        )
        .add(json()
          .set("a", 2)
          .set("b", 2)
        )
      )
      .set("c", json().add(1).add("a"));
    Json filter = json.filterFields("b.b, a");
    System.out.println(filter.toPretty());
    assertTrue(filter.get("a").isNumber());
    assertTrue(filter.get("b").get(0).get("a").isMissing());
    assertTrue(filter.get("b").get(0).get("b").isNumber());
    assertTrue(filter.get("b").get(1).get("a").isMissing());
    assertTrue(filter.get("b").get(1).get("b").isNumber());
  }

  @Test
  public void asMap() {
    Json json = json()
      .set("a", 1)
      .set("b", json()
        .set("a", 1)
        .set("b", 1)
      );
    Map<String, Json> map = json.as(Map.class, String.class, Json.class);
    Map<String, String> b = map.get("b").as(Map.class, String.class, String.class);
    assertEquals("1", b.get("a"));
  }


  @Test
  public void castToString() {
    assertEquals("1", json(1).asString());
  }
}
