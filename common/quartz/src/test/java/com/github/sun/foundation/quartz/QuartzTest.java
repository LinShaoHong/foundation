package com.github.sun.foundation.quartz;

import org.junit.Before;
import org.junit.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

public class QuartzTest {
  private Scheduler scheduler;
  private org.quartz.Scheduler quartz;

  @Before
  public void init() throws SchedulerException {
    StdSchedulerFactory factory = new StdSchedulerFactory();
    quartz = factory.getScheduler();
    quartz.setJobFactory((b, s) -> {
      Class<? extends Job> jobClass = b.getJobDetail().getJobClass();
      return newInstance(jobClass);
    });
    scheduler = new QuartzScheduler(quartz);
    scheduler.startup();
  }

  @Test
  public void test_schedule_once() {
    long now = System.currentTimeMillis();
    TaskImpl task = new TaskImpl();
    scheduler.scheduleOnce(new Date(now + 2000), task);
    sleep(40000);
  }

  @Test
  public void test_schedule() {
    long now = System.currentTimeMillis();
    TaskImpl task = new TaskImpl();
    scheduler.schedule(new Date(now + 2000), 3, Scheduler.CalendarUnit.SECONDS, task);
    sleep(40000);
  }

  @Test
  public void test_pause() throws SchedulerException {
    long now = System.currentTimeMillis();
    TaskImpl task = new TaskImpl();
    scheduler.schedule(new Date(now + 2000), 3, Scheduler.CalendarUnit.SECONDS, task);
    sleep(10000);
    quartz.pauseJob(new JobKey(task.id()));
    sleep(10000);
    quartz.resumeJob(new JobKey(task.id()));
    sleep(40000);
  }

  @Test
  public void test_delete() throws SchedulerException {
    long now = System.currentTimeMillis();
    TaskImpl task = new TaskImpl();
    scheduler.schedule(new Date(now + 2000), 3, Scheduler.CalendarUnit.SECONDS, task);
    sleep(10000);
    quartz.resumeJob(new JobKey(task.id()));
    sleep(10000);
  }

  public static class TaskImpl implements Scheduler.Task {
    private int counter = 1;

    @Override
    public void run() {
      while (counter < 3) {
        System.out.println(Thread.currentThread().getName() + " === " + (counter++));
        sleep(1000);
      }
    }

    @Override
    public Class<? extends org.quartz.Job> jobClass() {
      return JobAdapter.class;
    }
  }

  public static class JobAdapter implements org.quartz.Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        String taskClass = context.getJobDetail().getJobDataMap().get("$JOB_CLASS").toString();
        Scheduler.Task task = (Scheduler.Task) newInstance(Class.forName(taskClass));
        task.run();
      } catch (ClassNotFoundException ex) {
        throw new JobExecutionException(ex);
      }
    }
  }

  private static <T> T newInstance(Class<T> clazz) {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException ex) {
      ex.printStackTrace();
      throw new RuntimeException(ex);
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }
}
