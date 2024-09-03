package com.github.sun.foundation.modelling;

import java.lang.annotation.*;

/**
 * @Author LinSH
 * @Date: 11:12 AM 2019-03-01
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Converter {
    Class<? extends Handler> value();

    interface Handler<M, D> {
        D serialize(M value);

        M deserialize(D value);
    }

    interface Parser {
        Class<?> parse(Class<? extends Handler> handlerClass);
    }
}
