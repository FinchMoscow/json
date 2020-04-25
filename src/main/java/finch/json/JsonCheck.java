package finch.json;

import lombok.AllArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class JsonCheck {
  private final Json json;
  private final Function<Json, Object> fn;

  private boolean check() {
    Json r = Json.json(fn.apply(this.json));
    return !(r.isNull() || r.isMissing() || !r.asBoolean());
  }

  public Json orElse(Object value) {
    if (check()) {
      return json;
    }
    return Json.json(value);
  }

  public Json orElseGet(Supplier<Object> fn) {
    if (check()) {
      return json;
    }
    return Json.json(fn.get());
  }

  public Json orElseMap(Function<Json, Object> fn) {
    if (check()) {
      return json;
    }
    return Json.json(fn.apply(json));
  }

  public <X extends Throwable> Json orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (check()) {
      return json;
    } else {
      throw exceptionSupplier.get();
    }
  }
}
