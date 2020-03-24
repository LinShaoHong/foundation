package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class PropertyLoaders {
  public Properties loadProperties(String... resourcesPaths) {
    Properties props = new Properties();
    for (String location : resourcesPaths) {
      try (InputStream is = getInputStream(location)) {
        if (is != null) {
          props.load(is);
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return props;
  }

  private ClassLoader getDefaultClassLoader() {
    ClassLoader cl;
    try {
      cl = Thread.currentThread().getContextClassLoader();
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
    if (cl == null) {
      cl = PropertyLoaders.class.getClassLoader();
      if (cl == null) {
        try {
          cl = ClassLoader.getSystemClassLoader();
        } catch (Throwable ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    return cl;
  }

  private InputStream getInputStream(String path) throws IOException {
    return getDefaultClassLoader().getResourceAsStream(path);
  }
}
