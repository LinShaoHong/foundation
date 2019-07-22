package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.util.concurrent.Callable;

@UtilityClass
public class Retry {
  public <T> T execute(int maxCount, Callable<T> func) throws Exception {
    return execute(maxCount, 0, func);
  }

  public <T> T execute(int maxCount, long delayInMs, Callable<T> func) throws Exception {
    int counter = 1;
    maxCount = maxCount <= 0 ? 1 : maxCount;
    for (; ; ) {
      Thread.sleep(delayInMs);
      try {
        return func.call();
      } catch (Exception ex) {
        counter++;
        if (counter > maxCount) {
          throw ex;
        }
      }
    }
  }
}
