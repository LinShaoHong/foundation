package com.github.sun.foundation.mybatis.interceptor;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;

abstract class BasicInterceptor implements Interceptor {
  private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
  private static final DefaultObjectFactory OBJECT_FACTORY = new DefaultObjectFactory();
  private static final DefaultObjectWrapperFactory OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();

  MetaObject getMetaObject(Object obj) {
    MetaObject metaObject = MetaObject.forObject(obj, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
    while (metaObject.hasGetter("h")) {
      Object object = metaObject.getValue("h");
      metaObject = MetaObject.forObject(object, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
    }
    if (metaObject.hasGetter("target")) {
      Object target = metaObject.getValue("target");
      metaObject = MetaObject.forObject(target, OBJECT_FACTORY, OBJECT_WRAPPER_FACTORY, REFLECTOR_FACTORY);
      if (metaObject.hasGetter("h")) {
        return getMetaObject(target);
      }
    }
    return metaObject;
  }
}
