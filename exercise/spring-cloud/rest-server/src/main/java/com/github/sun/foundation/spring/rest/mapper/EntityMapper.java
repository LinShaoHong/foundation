package com.github.sun.foundation.spring.rest.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.spring.rest.entity.Entity;

public interface EntityMapper extends CompositeMapper<Entity> {
    int test();
}
