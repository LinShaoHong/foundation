package com.github.sun.foundation.sql;

import java.lang.annotation.*;

/**
 * @Author LinSH
 * @Date: 3:38 PM 2019-07-13
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NamingStrategy {
  String value() default CAMEL_CASE_TO_LOWER_UNDERSCORE;

  String CAMEL_CASE = "camelCase";
  String CAMEL_CASE_TO_LOWER_UNDERSCORE = "camelCaseToLowerUnderscore";
  String CAMEL_CASE_TO_UPPER_UNDERSCORE = "camelCaseToUpperUnderscore";
}
