package com.github.sun.foundation.quartz;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

@Order(Order.BACKGROUND_TASK)
public class TaskRunner implements Lifecycle {
  private Scheduler scheduler;

  @Override
  public void startup() {
    scheduler = new QuartzScheduler(quartz());
    scheduler.startup();
    Injector.interfaceOf(Scheduler.JobAdapter.class)
      .forEach(adapter -> {
        Rate rate = parseRate(adapter.rate());
        scheduler.schedule(adapter.start(), rate.value, rate.unit, adapter);
      });
  }

  @Override
  public void shutdown() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
  }

  private org.quartz.Scheduler quartz() {
    org.quartz.Scheduler scheduler;
    try {
      StdSchedulerFactory factory = new StdSchedulerFactory();
      scheduler = factory.getScheduler();
      scheduler.setJobFactory((b, s) -> Injector.getInstance(b.getJobDetail().getJobClass()));
      return scheduler;
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Rate parseRate(String rate) {
    rate = rate.trim();
    Scheduler.CalendarUnit unit;
    switch (rate.charAt(rate.length() - 1)) {
      case 's':
        unit = Scheduler.CalendarUnit.SECONDS;
        break;
      case 'm':
        unit = Scheduler.CalendarUnit.MINUTES;
        break;
      case 'h':
      case 'H':
        unit = Scheduler.CalendarUnit.HOURS;
        break;
      case 'D':
      case 'd':
        unit = Scheduler.CalendarUnit.DAYS;
        break;
      case 'M':
        unit = Scheduler.CalendarUnit.MONTHS;
        break;
      case 'Y':
      case 'y':
        unit = Scheduler.CalendarUnit.YEARS;
        break;
      default:
        throw new IllegalArgumentException();
    }
    int value = Integer.parseInt(rate.substring(0, rate.length() - 1));
    return new Rate(value, unit);
  }

  private static class Rate {
    private final int value;
    private final Scheduler.CalendarUnit unit;

    private Rate(int value, Scheduler.CalendarUnit unit) {
      this.value = value;
      this.unit = unit;
    }
  }
}
