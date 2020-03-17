package com.github.sun.foundation.rest;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public interface RequestScopeContextResolver<T> extends Factory<T> {
  T get();

  void remove();

  @Override
  default T provide() {
    return get();
  }

  @Override
  default void dispose(T instance) {
    remove();
  }

  interface BinderProvider {
    AbstractBinder binder();
  }
}
