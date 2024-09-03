package com.github.sun.foundation.modelling;

import java.lang.annotation.*;

/**
 * @Author LinSH
 * @Date: 11:24 AM 2019-03-01
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnPrefix {
    String value();
}
