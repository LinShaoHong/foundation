package com.github.sun.foundation.boot.utility;

/**
 * @Author LinSH
 * @Date: 11:37 AM 2019-07-11
 */
public class Configurators {
  public static String renderClasses(Iterable<Class<?>> classes) {
    StringBuilder sb = new StringBuilder();
    for (Class<?> c : classes) {
      sb.append("- ").append(c.getName()).append("\n");
    }
    return sb.toString();
  }
}
