# FINCH JSON

Finch-Json is a library designed to help you write easier null-safe code. It simplifies the syntax and reduces the amount of code lines in your work.

## How it works

It's just a wrapper around the [Jackson library](https://github.com/FasterXML/jackson) and it executes only Jackson's methods.    
 
## Usage:

Create or parse JSON:

```java
var createJson = json()
      .set("name", "Ivan")
      .set("addresses", array()
        .add(json()
          .set("street", "Popov pr 1k1")
          .set("city", "Moscow")
        )
      );

var parseJson = Json.parse("{\"name\":"Ivan", \"address\":[{\"street\": \"Popov pr 1k1\"}]}");
```

Use with [Spring](https://spring.io/)

```java
@PostMapping("/{entityId}")
public Json updateEntity(@PathVariable long entityId, @RequestBody Json data) { // data : {"updatedField": "new value"}
    var entity = repository.findById(entityId);
    data.updateObject(entity);
    repository.save(entity);      
}

```

## License

Finch-Json is released under the [MIT License](LICENSE.md).

## Special Thanks

- [Dmitriy Khayredinov](https://github.com/khayredinov)
- [Kirill A.](https://github.com/kirill-astapov)
- [Kirill S.](https://github.com/kirrok)



