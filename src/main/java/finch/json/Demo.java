package finch.json;

import lombok.Data;

import java.util.Properties;

import static finch.json.Json.json;
import static finch.json.Json.parse;

public class Demo {
  @Data
  static class A {
    private StringWrap a;
    private int b;
  }

  @Data
  static class B {
    private float b;
  }

  @Data
  public static class StringWrap implements ToJson {
    private String value;

    public StringWrap(Json json) {
      this.value = json.asString();
    }

    public StringWrap(String value) {
      this.value = value;
    }

    @Override
    public Json toJson() {
      return json(value);
    }
  }

  public static void main(String[] args) {
    Properties properties = json()
      .set("a", "1")
      .set("b", "2")
      .as(Properties.class);

    System.out.println("properties=" + properties);

    String pretty = json()
      .set("a", json()
        .add(1)
        .add(2)
        .add(json()
          .set("a", 1)
        )
      )
      .set("b", json().set("a", 1))
      .toPretty();
    System.out.println("pretty=" + pretty);


    B b = parse("{\"b\":0.1}").as(B.class);
    System.out.println("b=" + b);

    A a = json(b).set("a", "" + b.getB()).as(A.class);
    System.out.println("a=" + a);
  }
}
