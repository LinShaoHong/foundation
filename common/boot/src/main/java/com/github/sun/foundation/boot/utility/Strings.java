package com.github.sun.foundation.boot.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author LinSH
 * @Date: 10:43 AM 2019-03-01
 */
public class Strings {
  public static String camelCaseToUnderScore(String name) {
    StringBuilder sb = new StringBuilder();
    char[] arr = name.toCharArray();
    int i = 1;
    char prev, curr, next;
    sb.append(Character.toLowerCase(arr[i - 1]));
    while (i < arr.length - 1) {
      curr = arr[i];
      prev = arr[i - 1];
      next = arr[i + 1];
      if (Character.isUpperCase(curr) &&
        (Character.isLowerCase(prev) || Character.isLowerCase(next))) {
        sb.append("_");
      }
      sb.append(Character.toLowerCase(curr));
      i++;
    }
    if (i < arr.length) {
      sb.append(Character.toLowerCase(arr[i]));
    }
    return sb.toString();
  }

  public static String joinCamelCase(String... arr) {
    StringBuilder sb = new StringBuilder(arr[0]);
    for (int i = 1; i < arr.length; i++) {
      sb.append(arr[i].length() > 1 ?
        Character.toUpperCase(arr[i].charAt(0)) + arr[i].substring(1) : arr[i].toUpperCase());
    }
    return sb.toString();
  }

  public static String underScoreToCamelCase(String name) {
    if (name == null || name.isEmpty()) return name;
    String[] arr = name.toLowerCase().split("_");
    StringBuilder sb = new StringBuilder(arr[0]);
    for (int i = 1; i < arr.length; i++) {
      String s = arr[i];
      sb.append(Character.toUpperCase(s.charAt(0)));
      if (s.length() > 1) {
        sb.append(s.substring(1).toLowerCase());
      }
    }
    return sb.toString();
  }

  private static final Pattern pattern = Pattern.compile("-?[0-9]+");

  public static boolean isInt(String s) {
    if (s == null || s.isEmpty()) return false;
    return pattern.matcher(s).matches();
  }

  /**
   * https://docs.oracle.com/javase/tutorial/java/data/characters.html
   */
  public static String escape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, c = s.length(); i < c; i++) {
      char ch = s.charAt(i);
      switch (ch) {
        case 0:
          sb.append("\\0");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\"':
          sb.append("\\\"");
          break;
        case '\'':
          sb.append("\\\'");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\f':
          sb.append("\\f");
          break;
        default:
          sb.append(ch);
          break;
      }
    }
    return sb.toString();
  }

  public static Parser newParser() {
    return new Parser();
  }

  public static class Parser implements AutoCloseable {
    private int point;
    private int offset;
    private String input;
    private Map<Pattern, Matcher> matcher = new HashMap<>();

    public Parser set(String input) {
      this.offset = 0;
      this.input = input;
      matcher.values().forEach(m -> m.reset(input));
      return this;
    }

    private String next(Pattern pattern) {
      Matcher m = matcher.computeIfAbsent(pattern, x -> pattern.matcher(input));
      m.region(offset, input.length());
      if (m.lookingAt()) {
        point = offset;
        offset = m.end();
        return input.substring(point, offset);
      }
      return null;
    }

    public String left() {
      return input.substring(0, offset);
    }

    public String right() {
      return input.substring(offset);
    }

    public String processed() {
      return input.substring(point, offset);
    }

    public boolean skip(Pattern pattern) {
      return next(pattern) != null;
    }

    public boolean finished() {
      return offset == input.length();
    }

    @Override
    public void close() {
      this.matcher.clear();
      this.input = null;
      this.point = this.offset = 0;
    }
  }
}
