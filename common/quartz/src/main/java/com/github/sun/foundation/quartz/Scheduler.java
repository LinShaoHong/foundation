package com.github.sun.foundation.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;

import java.util.Calendar;
import java.util.Date;

public interface Scheduler {
  void scheduleOnce(Date start, JobAdapter adapter);

  void schedule(Date start, int rate, CalendarUnit unit, JobAdapter adapter);

  void startup();

  void shutdown();

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

  interface JobAdapter {
    String id();

    Date start();

    String rate();

    JobDataMap data();

    Class<? extends Job> jobClass();
  }
}
