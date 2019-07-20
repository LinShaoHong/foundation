package com.github.sun.foundation.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class QuartzScheduler implements Scheduler {
  private static String JOB_CLASS = "class";
  private final org.quartz.Scheduler quartz;

  public QuartzScheduler(org.quartz.Scheduler quartz) {
    this.quartz = quartz;
  }

  @Override
  public void scheduleOnce(Date start, JobAdapter adapter) {
    Date now = new Date();
    if (start.before(now)) {
      start = now;
    }
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    log.info("Start scheduled task once at " + format.format(start));
    schedule(start, null, adapter);
  }

  @Override
  public void schedule(Date start, int rate, CalendarUnit unit, JobAdapter adapter) {
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
    schedule(start, builder, adapter);
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

  private void schedule(Date start, ScheduleBuilder<? extends Trigger> builder, JobAdapter adapter) {
    JobDetail job = JobBuilder.newJob(adapter.jobClass())
      .withIdentity(adapter.id())
      .setJobData(adapter.data())
      .build();
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity(adapter.id())
      .startAt(start)
      .withSchedule(builder)
      .build();
    try {
      try {
        quartz.scheduleJob(job, trigger);
      } catch (ObjectAlreadyExistsException ex) {
        quartz.rescheduleJob(TriggerKey.triggerKey(adapter.id()), trigger);
      }
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }
}
