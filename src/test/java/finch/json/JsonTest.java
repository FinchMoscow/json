package finch.json;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static finch.json.Json.json;
import static finch.json.Json.parse;
import static org.junit.Assert.*;

public class JsonTest {

  @Test(expected = IllegalArgumentException.class)
  public void check() {
    assertTrue(Json.missing().check(Json::isBoolean).orElse(true).asBoolean());
    assertFalse(Json.json(false).check(Json::isBoolean).orElseGet(() -> true).asBoolean());
    assertTrue(Json.json(1).check(Json::isBoolean).orElseMap((j) -> true).asBoolean());
    Json.missing().check(Json::isBoolean).orElseThrow(IllegalArgumentException::new);
  }

  @Test
  public void removeNulls() {
    var json = json()
      .set("a", null)
      .set("b", json()
        .set("a", null)
      )
      .set("c", json()
        .set("a", null)
        .set("b", 1)
      );
    Json removeNulls = json.removeNulls();
    assertTrue(json.select("b").isObject());
    assertTrue(json.select("b.a").isNull());
    assertTrue(json.select("c").isObject());
    assertTrue(json.select("c.a").isNull());
    assertTrue(removeNulls.select("b").isMissing());
    assertTrue(removeNulls.select("c").isObject());
    assertTrue(removeNulls.select("c.a").isMissing());
  }

  @Test
  public void equalsAndHash() {
    var j1 = parse("{\"a\":1, \"b\":{\"c\":null}}");
    var j2 = parse("{\"b\":{\"c\":null}, \"a\":1}");
    var j3 = parse("{\"b\":{\"c\":null, \"d\":null}, \"a\":1}");
    assertEquals(j1, j2);
    assertEquals(j1.hashCode(), j2.hashCode());
    assertNotEquals(j1, j3);
    assertNotEquals(j1.hashCode(), j3.hashCode());
  }

  @Test
  public void changeFieldNameCase() {
    var json = json("superWRMLTest-aaa", 1);
    assertTrue(json.get("superWRMLTest-aaa").isNumber());
    assertTrue(json.toCamelCase().get("SuperWRMLTestAaa").isNumber());
    assertTrue(json.toKebabCase().get("super-wrml-test-aaa").isNumber());
    assertTrue(json.toLowerCamelCase().get("superWRMLTestAaa").isNumber());
    assertTrue(json.toScreamingSnakeCase().get("SUPER_WRML_TEST_AAA").isNumber());
    assertTrue(json.toSnakeCase().get("super_wrml_test_aaa").isNumber());
    assertTrue(json.toTrainCase().get("super-wrml-test-aaa").isNumber());
  }

  @Test
  public void fixStructure() {
    var json = json()
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
    var json = json()
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
    var filter = json.filterFields("b.b, a");
    assertTrue(filter.get("a").isNumber());
    assertTrue(filter.get("b").get(0).get("a").isMissing());
    assertTrue(filter.get("b").get(0).get("b").isNumber());
    assertTrue(filter.get("b").get(1).get("a").isMissing());
    assertTrue(filter.get("b").get(1).get("b").isNumber());
  }

  @Test
  public void asMap() {
    var json = json()
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

  @Test
  public void pushCommand() {
    var person = Examples.Person.builder()
      .addAddress(new Examples.Address("Baker", "London"))
      .build();

    var secondAddress = new Examples.Address("Lenina", "Moscow");

    var json = Json.json(person);
    assertFalse(json.as(Examples.Person.class).getAddresses().contains(secondAddress));

    var pushJson = Json.json("$push", Json.json("addresses", secondAddress));
    json.op(pushJson);
    assertTrue(json.as(Examples.Person.class).getAddresses().contains(secondAddress));
  }

  @Test
  public void updateObject(){
    var name = "Ivan";
    var person = Examples.Person.builder()
      .name(name)
      .phone("123")
      .build();

    var updateJson = Json.parse("{\"phone\":\"234\"}");
    updateJson.updateObject(person);

    Assert.assertEquals(name, person.getName());
    Assert.assertEquals("234", person.getPhone());
  }
}
