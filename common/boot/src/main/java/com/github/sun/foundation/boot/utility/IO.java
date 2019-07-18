package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@UtilityClass
public class IO {
  public String read(InputStream in) {
    StringBuilder sb = new StringBuilder();
    try (InputStreamReader reader = new InputStreamReader(in)) {
      char[] buf = new char[2048];
      int c;
      for (; ; ) {
        c = reader.read(buf);
        if (c < 0) break;
        if (c > 0) {
          sb.append(new String(buf, 0, c).intern());
        }
      }
      return sb.toString();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
