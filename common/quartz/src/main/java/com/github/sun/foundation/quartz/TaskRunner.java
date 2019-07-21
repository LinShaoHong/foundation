package com.github.sun.foundation.quartz;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;

@Order(Order.BACKGROUND_TASK)
public class TaskRunner implements Lifecycle {
  private Scheduler scheduler;

  @Override
  public void startup() {
    scheduler = Injector.getInstance(Scheduler.class);
    scheduler.startup();
    Injector.interfaceOf(Scheduler.Task.class)
      .forEach(task -> {
        Scheduler.Rate rate = Scheduler.parseRate(task.rate());
        scheduler.schedule(task.start(), rate.value, rate.unit, task);
      });
  }

  @Override
  public void shutdown() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }
}
