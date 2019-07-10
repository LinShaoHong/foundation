package com.github.sun.foundation.mybatis.interceptor;

import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @Author LinSH
 * @Date: 12:27 PM 2019-07-10
 */
public class BasicInterceptor {
  private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
  private static final DefaultObjectFactory OBJECT_FACTORY = new DefaultObjectFactory();
  private static final DefaultObjectWrapperFactory OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();

  protected MetaObject getMetaObject(Object obj) {
    ObjectFactory objectFactory = OBJECT_FACTORY;
    ObjectWrapperFactory objectWrapperFactory = OBJECT_WRAPPER_FACTORY;
    MetaObject metaObject = MetaObject.forObject(obj, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
    while (metaObject.hasGetter("h")) {
      Object object = metaObject.getValue("h");
      metaObject = MetaObject.forObject(object, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
    }
    if (metaObject.hasGetter("target")) {
      Object target = metaObject.getValue("target");
      metaObject = MetaObject.forObject(target, objectFactory, objectWrapperFactory, REFLECTOR_FACTORY);
      if (metaObject.hasGetter("h")) {
        return getMetaObject(target);
      }
    }
    return metaObject;
  }
}
