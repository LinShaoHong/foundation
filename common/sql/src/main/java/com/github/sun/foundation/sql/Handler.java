package com.github.sun.foundation.sql;

import java.lang.annotation.*;

/**
 * @Author LinSH
 * @Date: 11:12 AM 2019-03-01
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {
  Class<?> value();
}
