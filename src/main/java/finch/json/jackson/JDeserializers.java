package finch.json.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import finch.json.Json;

import java.io.IOException;
import java.lang.reflect.*;

public class JDeserializers extends com.fasterxml.jackson.databind.deser.Deserializers.Base {

  @Override
  public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription beanDesc) throws JsonMappingException {
    try {
      Constructor<?> constructor = type.getRawClass().getConstructor(Json.class);

      return new JsonDeserializer<Object>() {
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
          try {
            return constructor.newInstance(Json.json(p.readValueAsTree()));
          } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        }
      };
    } catch (NoSuchMethodException e) {
      //ignore
    }
    if (type.isInterface()) {
      return new JsonDeserializer<Object>() {
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
          return map(type.getRawClass(), p.readValueAsTree());
        }
      };
    }
    return super.findBeanDeserializer(type, config, beanDesc);
  }

  public static Object map(Class<?> klaus, final ObjectNode data) {
    return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{klaus}, new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String property = method.getName().substring(3);
        property = new String(new char[]{property.charAt(0)}).toLowerCase() + property.substring(1);
        if(method.getDeclaringClass().equals(Object.class)) {
          return method.invoke(data, args);
        }
        // getter
        if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
          return getProperty(data, property, method);
        }

        // setter
        if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
          data.putPOJO(property, args[0]);
          return null;
        }

        throw new RuntimeException("Not a property: " + method.getName());
      }
    });
  }

  private static Object getProperty(ObjectNode data, String property, Method method) {
    Class<?> returnType = method.getReturnType();
    JsonNode value = data.get(property);

    if (Integer.class.equals(returnType) || int.class.equals(returnType)) {
      return value.asInt();
    } else if (Double.class.equals(returnType) || double.class.equals(returnType)) {
      return value.asDouble();
    } else if (Long.class.equals(returnType) || long.class.equals(returnType)) {
      return value.asLong();
    } else if (Boolean.class.equals(returnType) || boolean.class.equals(returnType)) {
      return value.asBoolean();
    } else if (String.class.equals(returnType)) {
      return value.asText();
    } else if (returnType.isInterface()) {
      if (value.isObject()) {
        return map(returnType, (ObjectNode) value);
      } else if (value.isPojo()) {
        return ((POJONode) value).getPojo();
      }
    }

    throw new RuntimeException("Unknown return type: " + returnType);
  }
}
