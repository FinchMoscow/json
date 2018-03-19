package finch.json;

import org.junit.Test;

import static finch.json.Json.json;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonTest {
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
      );
    Json filter = json.filter("b.b, a");
    assertTrue(filter.get("a").isNumber());
    assertTrue(filter.get("b").get(0).get("a").isNull());
    assertTrue(filter.get("b").get(0).get("b").isNumber());
    assertTrue(filter.get("b").get(1).get("a").isNull());
    assertTrue(filter.get("b").get(1).get("b").isNumber());
  }
}
