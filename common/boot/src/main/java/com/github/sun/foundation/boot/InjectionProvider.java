package com.github.sun.foundation.boot;

import java.util.HashMap;
import java.util.Map;

public interface InjectionProvider {
    void config(Binder binder);

    interface Binder {
        Binder named(String name);

        void bind(Object bean);

        void bind(Class<?> beanClass);

        Property addProperty(String name, Object bean);
    }

    interface Property {
        Property addProperty(String name, Object bean);

        void toClass(Class<?> beanClass);
    }

    class BinderImpl implements Binder, Property {
        private String name;
        private final Map<String, Object> properties = new HashMap<>();

        @Override
        public Binder named(String name) {
            this.name = name;
            return this;
        }

        @Override
        public void bind(Object bean) {
            if (name != null && !name.isEmpty()) {
                Injector.inject(name, bean);
                name = null;
            } else {
                Injector.inject(bean);
            }
        }

        @Override
        public void bind(Class<?> beanClass) {
            if (name != null) {
                Injector.inject(name, beanClass);
                name = null;
            } else {
                Injector.inject(beanClass);
            }
        }

        @Override
        public Property addProperty(String name, Object bean) {
            properties.put(name, bean);
            return this;
        }

        @Override
        public void toClass(Class<?> beanClass) {
            if (name != null) {
                Injector.inject(name, beanClass, properties);
                name = null;
            } else {
                Injector.inject(beanClass, properties);
            }
            properties.clear();
        }
    }
}
