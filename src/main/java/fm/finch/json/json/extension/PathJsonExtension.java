package fm.finch.json.json.extension;

import fm.finch.json.json.Json;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Be careful to use these methods you must to include spring-core to your classpath.
 * @see <a href="https://mvnrepository.com/artifact/org.springframework/spring-core">Spring core</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathJsonExtension {

  private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
  private static final int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Scans a classpath and creates a JSON where keys are matched locations and values are read files.
   * @param locationPattern Ant-style location pattern
   * @return the JSON with parsed files
   * @throws IOException
   */
  public static Json read(String locationPattern) throws IOException {
    PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
    try {
      String root = pathMatchingResourcePatternResolver.getResource(locationPattern).getURI().toString();
      Json json = Json.json();
      for (Resource resource : pathMatchingResourcePatternResolver.getResources(locationPattern + "/**/*.json")) {
        String path = jsonPath(root, resource);

        Json value = Json.parse(read(resource.getInputStream()));
        json.select(path).set(value);
      }
      return json;
    } catch (FileNotFoundException exception) {
      return Json.missing();
    }
  }

  public static String read(InputStream is) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(is);
    byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
    int capacity = buf.length;
    int nread = 0;
    int n;
    for (; ; ) {
      // read to EOF which may read more or less than initial buffer size
      while ((n = bis.read(buf, nread, capacity - nread)) > 0)
        nread += n;

      // if the last call to read returned -1, then we're done
      if (n < 0)
        break;

      // need to allocate a larger buffer
      if (capacity <= MAX_BUFFER_SIZE - capacity) {
        capacity = capacity << 1;
      } else {
        if (capacity == MAX_BUFFER_SIZE)
          throw new OutOfMemoryError("Required array size too large");
        capacity = MAX_BUFFER_SIZE;
      }
      buf = Arrays.copyOf(buf, capacity);
    }
    return new String((capacity == nread) ? buf : Arrays.copyOf(buf, nread), StandardCharsets.UTF_8);
  }

  private static String jsonPath(String root, Resource resource) throws IOException {
    return Arrays.stream(
      resource.getURI().toString()
        .substring(root.length())
        .split("/")
    )
      .map(s -> s.split("\\.")[0])
      .filter(s -> s.length() > 0)
      .collect(Collectors.joining("."));
  }
}
