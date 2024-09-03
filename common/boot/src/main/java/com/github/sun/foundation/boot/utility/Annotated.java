package com.github.sun.foundation.boot.utility;

import java.lang.annotation.Annotation;

public interface Annotated {
    <T extends Annotation> T annotationOf(Class<T> annotationClass);

    <T extends Annotation> T[] annotationsOf(Class<T> annotationClass);

    default <T extends Annotation> boolean hasAnnotation(Class<T> annotationClass) {
        return annotationOf(annotationClass) != null;
    }
}
