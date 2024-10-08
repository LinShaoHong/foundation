package com.github.sun.foundation.quartz;

import com.github.sun.foundation.boot.InjectionProvider;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.boot.utility.Cache;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class QuartzScheduler implements Scheduler {
    private final org.quartz.Scheduler quartz;

    public QuartzScheduler(org.quartz.Scheduler quartz) {
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
    public void rescheduleOnce(Date start, String taskId) {
        Date now = new Date();
        if (start.before(now)) {
            start = now;
        }
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("Start rescheduled task once at " + format.format(start));
        reschedule(start, null, taskId);
    }

    @Override
    public void schedule(Date start, int rate, CalendarUnit unit, Task task) {
        CalendarIntervalScheduleBuilder builder = scheduleBuilder(rate, unit);
        start = getStartTime(start, unit, rate);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("Start scheduling task periodically every " + rate + " " + unit.toString().toLowerCase() + " from " + format.format(start));
        schedule(start, builder, task);
    }

    @Override
    public void reschedule(Date start, int rate, CalendarUnit unit, String taskId) {
        CalendarIntervalScheduleBuilder builder = scheduleBuilder(rate, unit);
        start = getStartTime(start, unit, rate);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("Start rescheduling task periodically every " + rate + " " + unit.toString().toLowerCase() + " from " + format.format(start));
        reschedule(start, builder, taskId);
    }

    private CalendarIntervalScheduleBuilder scheduleBuilder(int rate, CalendarUnit unit) {
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
        return builder;
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

    private void reschedule(Date start, ScheduleBuilder<? extends Trigger> builder, String taskId) {
        TriggerKey triggerKey = new TriggerKey(taskId);
        Trigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(taskId)
                .startAt(start)
                .withSchedule(builder)
                .build();
        try {
            quartz.rescheduleJob(triggerKey, newTrigger);
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
                org.quartz.Scheduler quartz = factory.getScheduler();
                quartz.setJobFactory((b, s) -> {
                    Class<? extends Job> jobClass = b.getJobDetail().getJobClass();
                    return cache.get(jobClass, () -> newInstance(jobClass));
                });
                binder.bind(quartz);
                binder.bind(new QuartzScheduler(quartz));
            } catch (SchedulerException ex) {
                throw new RuntimeException(ex);
            }
        }

        private <T> T newInstance(Class<T> clazz) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Order(Order.BACKGROUND_TASK)
    public static class TaskRunner implements Lifecycle {
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
}
