package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.TypeInfo;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.lang.reflect.Type;

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

  abstract class AbstractProvider<V, T> implements BinderProvider {
    @Override
    @SuppressWarnings("unchecked")
    public AbstractBinder binder() {
      Type contextType = TypeInfo.getTypeParameters(getClass(), AbstractProvider.class).get(0);
      Type providedType = TypeInfo.getTypeParameters(getClass(), AbstractProvider.class).get(1);
      return new AbstractBinder() {
        @Override
        protected void configure() {
          bindFactory((Class<? extends Factory<T>>) contextType).to((Class<T>) providedType);
        }
      };
    }
  }
}
