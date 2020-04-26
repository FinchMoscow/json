package fm.finch.json.json.utils;

import fm.finch.json.json.utils.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

  @Test
  public void capitalize() {
    Assert.assertNull(StringUtils.capitalize(null));
    Assert.assertEquals("", StringUtils.capitalize(""));
    Assert.assertEquals("Abc", StringUtils.capitalize("abc"));
  }

  @Test
  public void unCapitalize() {
    Assert.assertNull(StringUtils.unCapitalize(null));
    Assert.assertEquals("", StringUtils.unCapitalize(""));
    Assert.assertEquals("abc", StringUtils.unCapitalize("Abc"));
  }
}
