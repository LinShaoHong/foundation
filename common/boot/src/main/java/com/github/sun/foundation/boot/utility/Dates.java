package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;

@UtilityClass
public class Dates {
  private final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  public String format(Date date) {
    return FORMATTER.format(date);
  }

  private final long ONE_MINUTE = 60000L;
  private final long ONE_HOUR = 3600000L;
  private final long ONE_DAY = 86400000L;
  private final long ONE_WEEK = 604800000L;

  private final String ONE_SECOND_AGO = "秒前";
  private final String ONE_MINUTE_AGO = "分鍾前";
  private final String ONE_HOUR_AGO = "小時前";
  private final String ONE_DAY_AGO = "天前";
  private final String ONE_MONTH_AGO = "月前";
  private final String ONE_YEAR_AGO = "年前";

  public String simpleTime(Date date) {
    long delta = new Date().getTime() - date.getTime();
    if (delta < ONE_MINUTE) {
      long seconds = toSeconds(delta);
      return (seconds <= 0 ? 1 : seconds) + ONE_SECOND_AGO;
    }
    if (delta < 45L * ONE_MINUTE) {
      long minutes = toMinutes(delta);
      return (minutes <= 0 ? 1 : minutes) + ONE_MINUTE_AGO;
    }
    if (delta < 24L * ONE_HOUR) {
      long hours = toHours(delta);
      return (hours <= 0 ? 1 : hours) + ONE_HOUR_AGO;
    }
    if (delta < 48L * ONE_HOUR) {
      return "昨天";
    }
    if (delta < 30L * ONE_DAY) {
      long days = toDays(delta);
      return (days <= 0 ? 1 : days) + ONE_DAY_AGO;
    }
    if (delta < 12L * 4L * ONE_WEEK) {
      long months = toMonths(delta);
      return (months <= 0 ? 1 : months) + ONE_MONTH_AGO;
    } else {
      long years = toYears(delta);
      return (years <= 0 ? 1 : years) + ONE_YEAR_AGO;
    }
  }

  private long toSeconds(long date) {
    return date / 1000L;
  }

  private long toMinutes(long date) {
    return toSeconds(date) / 60L;
  }

  private long toHours(long date) {
    return toMinutes(date) / 60L;
  }

  private long toDays(long date) {
    return toHours(date) / 24L;
  }

  private long toMonths(long date) {
    return toDays(date) / 30L;
  }

  private long toYears(long date) {
    return toMonths(date) / 365L;
  }

  public String formatTime(long millis) {
    StringBuilder sb = new StringBuilder();
    long days = millis / (3600 * 24 * 1000);
    if (days > 0) {
      sb.append(days).append(" 天 ");
      millis = millis % (3600 * 24 * 1000);
    }
    long hours = millis / (3600 * 1000);
    if (hours > 0) {
      sb.append(hours).append(" 时 ");
      millis = millis % (3600 * 1000);
    }
    long minutes = millis / (60 * 1000);
    if (minutes > 0) {
      sb.append(minutes).append(" 分 ");
      millis = millis % (60 * 1000);
    }
    long seconds = millis / 1000;
    if (seconds > 0) {
      sb.append(seconds).append(" 秒 ");
    }
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
    }
    return sb.toString();
  }
}
