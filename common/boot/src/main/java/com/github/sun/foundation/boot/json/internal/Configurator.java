package com.github.sun.foundation.boot.json.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.github.sun.foundation.boot.json.ObjectMapperConfigurator;

import java.util.TimeZone;

public class Configurator implements ObjectMapperConfigurator {
    @Override
    public void config(ObjectMapper mapper) {
        // 支持构造方法传参
        mapper.registerModule(new ParanamerModule());
        // 支持java8类型
        mapper.registerModule(new Jdk8Module());
        // 时区
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }
}
