package finch.json;

import lombok.*;
import org.junit.Test;

import java.util.List;

import static finch.json.Json.array;
import static finch.json.Json.json;

public class Examples {
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class Person {
    private String name;
    @Singular("addAddress")
    private List<Address> addresses;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class Address {
    private String street;
    private String city;
  }

  interface PersonProjection {
    String getName();
  }

  public Person buildPojo() {
    return Person.builder()
      .name("Ivan")
      .addAddress(Address.builder()
        .city("Moscow")
        .street("Popov pr 1k1")
        .build())
      .build();
  }

  /**
   * DSL for build json
   */
  public Json buildJson() {
    return json()
      .set("name", "Ivan")
      .set("addresses", array()
        .add(json()
          .set("street", "Popov pr 1k1")
          .set("city", "Moscow")
        )
      );
  }

  @Test
  public void example() {
    // filter fields by interface
    Json json = buildJson();
    System.out.println(
      json
        .filterFields(PersonProjection.class)
        .toPretty()
    );
    // query
    System.out.println(json.get("addresses").get(0).get("street"));
    System.out.println(json.get("addresses").get(0).get("street1").isNull());
    System.out.println(json.get("addresses").get(0).get("street1").isMissing());
    // branching
    System.out.println(
      json.get("addresses")
        .check(j -> j.size() > 1)
        .orElseMap(j -> j.add(json()
            .set("street", "Popov pr 1k1")
            .set("city", "Moscow")
          )
        )
      .toPretty()
    );
  }
}
