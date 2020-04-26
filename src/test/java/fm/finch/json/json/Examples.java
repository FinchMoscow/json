package fm.finch.json.json;

import fm.finch.json.json.Json;
import lombok.*;
import org.junit.Test;

import java.util.List;

import static fm.finch.json.json.Json.array;
import static fm.finch.json.json.Json.json;

public class Examples {
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class Person {
    private String name;
    private String phone;
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
    Json json = buildJson();
    // filter fields by interface
    System.out.println(
      json
        .filterFields(PersonProjection.class)
        .toPretty()
    );
    // filter fields by name
    System.out.println(
      json
        .filterFields("name, addresses.street")
        .toPretty()
    );

    // query, iterate array
    for (Json address : json.get("addresses")) {
      address.isObject();
      address.get("street");
      address.get("street").isString();
      address.get("street1").isNull();
      address.get("street1").isMissing();
    }
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
