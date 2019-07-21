package com.github.sun.foundation.quartz;

import com.github.sun.foundation.boot.InjectionProvider;
import com.github.sun.foundation.boot.utility.Cache;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class QuartzScheduler implements Scheduler {
  private final org.quartz.Scheduler quartz;

  private QuartzScheduler(org.quartz.Scheduler quartz) {
    this.quartz = quartz;
  }

  @Override
  public void scheduleOnce(Date start, Task task) {
    Date now = new Date();
    if (start.before(now)) {
      start = now;
    }
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    log.info("Start scheduled task once at " + format.format(start));
    schedule(start, null, task);
  }

  @Override
  public void schedule(Date start, int rate, CalendarUnit unit, Task task) {
    CalendarIntervalScheduleBuilder builder = CalendarIntervalScheduleBuilder.calendarIntervalSchedule();
    switch (unit) {
      case YEARS:
        builder.withIntervalInYears(rate);
        break;
      case MONTHS:
        builder.withIntervalInMonths(rate);
        break;
      case DAYS:
        builder.withIntervalInDays(rate);
        break;
      case HOURS:
        builder.withIntervalInHours(rate);
        break;
      case MINUTES:
        builder.withIntervalInMinutes(rate);
        break;
      case SECONDS:
        builder.withIntervalInSeconds(rate);
        break;
    }
    start = getStartTime(start, unit, rate);
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    log.info("Start scheduling task periodically every " + rate + " " + unit.toString().toLowerCase() + " from " + format.format(start));
    schedule(start, builder, task);
  }

  @Override
  public void startup() {
    try {
      quartz.start();
    } catch (SchedulerException ex) {
      log.error("Error while startup quartz", ex);
    }
  }

  @Override
  public void shutdown() {
    try {
      quartz.shutdown(true);
    } catch (SchedulerException ex) {
      log.error("Error while shutdown quartz", ex);
    }
  }

  private Date getStartTime(Date start, CalendarUnit unit, int rate) {
    Calendar now = Calendar.getInstance();
    Calendar c = Calendar.getInstance();
    c.setTime(start);
    for (CalendarUnit u : CalendarUnit.values()) {
      if (u.calendarField > unit.calendarField) {
        break;
      }
      c.set(u.calendarField, now.get(u.calendarField));
    }
    // 如果已经过了，设置开始时间在下一个周期。如果不这样做，quartz会立即启动任务
    if (c.before(now)) {
      c.set(unit.calendarField, now.get(unit.calendarField) + rate);
    }
    return c.getTime();
  }

  private void schedule(Date start, ScheduleBuilder<? extends Trigger> builder, Task task) {
    JobDetail detail = JobBuilder.newJob(task.jobClass())
      .withIdentity(task.id())
      .setJobData(task.data())
      .build();
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity(task.id())
      .startAt(start)
      .withSchedule(builder)
      .build();
    try {
      try {
        quartz.scheduleJob(detail, trigger);
      } catch (ObjectAlreadyExistsException ex) {
        quartz.rescheduleJob(TriggerKey.triggerKey(task.id()), trigger);
      }
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static class ScheduleInjectionProvider implements InjectionProvider {
    private static final Cache<Class<? extends Job>, Job> cache = new Cache<>();

    @Override
    public void config(Binder binder) {
      try {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        org.quartz.Scheduler scheduler = factory.getScheduler();
        scheduler.setJobFactory((b, s) -> {
          Class<? extends Job> jobClass = b.getJobDetail().getJobClass();
          return cache.get(jobClass, () -> newInstance(jobClass));
        });
        binder.bind(new QuartzScheduler(scheduler));
      } catch (SchedulerException ex) {
        throw new RuntimeException(ex);
      }
    }

    private <T> T newInstance(Class<T> clazz) {
      try {
        return clazz.newInstance();
      } catch (InstantiationException | IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
