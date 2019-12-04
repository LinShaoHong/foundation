package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

/**
 * @Author LinSH
 * @Date: 11:30 AM 2019-07-11
 */
@UtilityClass
public class Packages {
  public String group(Class<?> c) {
    String name = c.getPackage().getName();
    String[] names = name.split("\\.");
    return names.length > 3 ? String.join(".", names[0], names[1], names[2]) : name;
  }
}
