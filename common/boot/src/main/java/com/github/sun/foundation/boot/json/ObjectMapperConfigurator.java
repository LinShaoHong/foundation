package com.github.sun.foundation.boot.json;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperConfigurator {
    void config(ObjectMapper mapper);
}
