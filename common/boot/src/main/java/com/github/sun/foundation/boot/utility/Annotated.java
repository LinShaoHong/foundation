package com.github.sun.foundation.boot.utility;

import java.lang.annotation.Annotation;

/**
 * @Author LinSH
 * @Date: 3:47 PM 2019-03-02
 */
public interface Annotated {
  <T extends Annotation> T annotationOf(Class<T> annotationClass);

  <T extends Annotation> T[] annotationsOf(Class<T> annotationClass);

  default <T extends Annotation> boolean hasAnnotation(Class<T> annotationClass) {
    return annotationOf(annotationClass) != null;
  }
}
