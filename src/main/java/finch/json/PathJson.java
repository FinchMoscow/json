package finch.json;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class PathJson {
  @SneakyThrows
  public static Json read(String locationPattern) {
    PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
    String root = pathMatchingResourcePatternResolver.getResource(locationPattern).getURI().toString();
    Json json = Json.json();
    for (Resource resource : pathMatchingResourcePatternResolver.getResources(locationPattern + "/*/*.json")) {
      String path = jsonPath(root, resource);
      Json value = Json.parse(new String(Files.readAllBytes(resource.getFile().toPath())));
      json.select(path).set(value);
    }
    return json;
  }

  private static String jsonPath(String root, Resource resource) throws IOException {
    return Lists
      .newArrayList(
        resource.getURI().toString()
          .substring(root.length())
          .split("\\/")
      )
      .stream()
      .map(s -> s.split("\\.")[0])
      .filter(s -> s.length() > 0)
      .collect(Collectors.joining("."));
  }
}
