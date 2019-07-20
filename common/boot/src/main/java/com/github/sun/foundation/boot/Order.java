package com.github.sun.foundation.boot;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
  int value() default DEFAULT;

  /**
   * 系统级模块，优先于业务级模块。
   */
  int SYSTEM = 1000;
  /**
   * 资源池，如线程池、连接池。
   */
  int RESOURCE_POOL = SYSTEM + 1000;
  /**
   * 内部组件。
   */
  int COMPONENT = SYSTEM + 2000;
  /**
   * 内部服务，如rpc服务器。
   */
  int INTERNAL_SERVICE = SYSTEM + 3000;
  /**
   * 后台任务，如定时任务等。
   */
  int BACKGROUND_TASK = SYSTEM + 4000;
  /**
   * 开放服务，如http服务器。
   */
  int OPEN_SERVICE = SYSTEM + 5000;
  /**
   * 业务级模块
   */
  int DEFAULT = OPEN_SERVICE + 1000;
}
