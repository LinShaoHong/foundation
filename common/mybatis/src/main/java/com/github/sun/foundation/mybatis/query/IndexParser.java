package com.github.sun.foundation.mybatis.query;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class IndexParser {
    public List<String> parseKeyName(String name) {
        if (name.startsWith("by") || name.startsWith("findBy")) {
            List<String> arr = new ArrayList<>();
            int i, j;
            i = j = name.startsWith("by") ? 2 : 6;
            while (i < name.length()) {
                j = name.indexOf("And", j);
                if (j < 0) {
                    j = name.length();
                } else {
                    if (j + 3 >= name.length()) {
                        throw new IllegalArgumentException("Indexed method name must like 'byKey1AndKey2AndKey3.....' : " + name);
                    }
                    if (!Character.isUpperCase(name.charAt(j + 3))) {
                        j = j + 3;
                        continue;
                    }
                }
                String field = name.substring(i, i + 1).toLowerCase() + name.substring(i + 1, j);
                arr.add(parseField(field));
                j = j + 3;
                i = j;
            }
            return arr;
        }
        return Collections.emptyList();
    }

    private String parseField(String field) {
        for (String suffix : Arrays.asList("Ge", "Gt", "Le", "Lt", "Ne", "In", "StartsWith")) {
            int i = field.lastIndexOf(suffix);
            if (i > 0) {
                return field.substring(0, i);
            }
        }
        return field;
    }
}
