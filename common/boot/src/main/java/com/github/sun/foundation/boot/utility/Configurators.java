package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Configurators {
    public String renderClasses(Iterable<Class<?>> classes) {
        StringBuilder sb = new StringBuilder();
        for (Class<?> c : classes) {
            sb.append("- ").append(c.getName()).append("\n");
        }
        return sb.toString();
    }
}
