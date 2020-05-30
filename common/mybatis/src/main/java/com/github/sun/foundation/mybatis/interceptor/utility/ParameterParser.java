package com.github.sun.foundation.mybatis.interceptor.utility;

import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import com.github.sun.foundation.sql.spi.SqlTemplate;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.apache.ibatis.annotations.Param;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class ParameterParser {
  private final Cache<Method, List<ParamAnnotation>> cache = new Cache<>();

  @SuppressWarnings("unchecked")
  public void parse(Method method, Map args) {
    List<ParamAnnotation> pas = cache.get(method, () -> parse(method));
    pas.forEach(pa -> {
      if (pa.getFlatten() != null && pa.getParam() != null) {
        String param = pa.getParam().value();
        Object value = args.get(param);
        if (value != null && Collection.class.isAssignableFrom(value.getClass())) {
          String flattenValue = null;
          if (!((Collection<?>) value).isEmpty()) {
            flattenValue = Iterators.mkString((Collection<?>) value, ", ", SqlTemplate::literal);
          }
          args.put(param, flattenValue);
          int index = pas.indexOf(pa) + 1;
          if (args.containsKey("param" + index)) {
            args.put("param" + index, flattenValue);
          }
        }
      }
    });
  }

  private List<ParamAnnotation> parse(Method method) {
    return Stream.of(method.getParameters())
      .map(parameter -> ParamAnnotation.builder()
        .param(parameter.getAnnotation(Param.class))
        .flatten(parameter.getAnnotation(Flatten.class))
        .build())
      .collect(Collectors.toList());
  }

  @Data
  @Builder
  private static class ParamAnnotation {
    private Param param;
    private Flatten flatten;
  }
}
