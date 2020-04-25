package finch.json.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.UnaryOperator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {

  public static String capitalize(String value) {
    return changeFirstCharacter(value, Character::toUpperCase);
  }

  public static String unCapitalize(String value) {
    return changeFirstCharacter(value, Character::toLowerCase);
  }

  private static String changeFirstCharacter(String value, UnaryOperator<Character> fn) {
    if(value == null || value.length() == 0) {
      return value;
    }

    char[] chars = value.toCharArray();
    chars[0] = fn.apply(chars[0]);

    return new String(chars);
  }

}
