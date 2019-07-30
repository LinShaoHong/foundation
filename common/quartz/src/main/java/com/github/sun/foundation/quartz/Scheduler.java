package com.github.sun.foundation.quartz;

import com.github.sun.foundation.boot.Injector;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;
import java.util.Date;

public interface Scheduler {
  void scheduleOnce(Date start, Task task);

  void rescheduleOnce(Date start, String taskId);

  void schedule(Date start, int rate, CalendarUnit unit, Task task);

  void reschedule(Date start, int rate, CalendarUnit unit, String taskId);

  boolean has(String taskId);

  void pause(String taskId);

  void resume(String taskId);

  void delete(String taskId);

  void startup();

  void shutdown();

  interface Task extends Runnable {
    default String id() {
      return getClass().getName();
    }

    default Date start() {
      return null;
    }

    default String rate() {
      return null;
    }

    default JobDataMap data() {
      JobDataMap data = new JobDataMap();
      data.put("$JOB_CLASS", id());
      return data;
    }

    default Class<? extends org.quartz.Job> jobClass() {
      return JobAdapter.class;
    }
  }

  class JobAdapter implements org.quartz.Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        String taskClass = context.getJobDetail().getJobDataMap().get("$JOB_CLASS").toString();
        Task task = (Task) Injector.getInstance(Class.forName(taskClass));
        task.run();
      } catch (ClassNotFoundException ex) {
        throw new JobExecutionException(ex);
      }
    }
  }

  enum CalendarUnit {
    YEARS(Calendar.YEAR),
    MONTHS(Calendar.MONTH),
    DAYS(Calendar.DATE),
    HOURS(Calendar.HOUR_OF_DAY),
    MINUTES(Calendar.MINUTE),
    SECONDS(Calendar.SECOND);

    public final int calendarField;

    CalendarUnit(int calendarField) {
      this.calendarField = calendarField;
    }
  }

  static Rate parseRate(String rate) {
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

  class Rate {
    public final int value;
    public final CalendarUnit unit;

    private Rate(int value, CalendarUnit unit) {
      this.value = value;
      this.unit = unit;
    }
  }
}
