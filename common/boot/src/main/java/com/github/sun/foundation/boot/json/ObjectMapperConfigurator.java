package com.github.sun.foundation.boot.json;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Author LinSH
 * @Date: 3:48 PM 2019-07-11
 */
public interface ObjectMapperConfigurator {
  void config(ObjectMapper mapper);
}
