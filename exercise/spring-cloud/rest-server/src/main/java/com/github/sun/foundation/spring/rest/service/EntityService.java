package com.github.sun.foundation.spring.rest.service;

import com.github.sun.foundation.spring.rest.entity.Entity;
import com.github.sun.foundation.spring.rest.mapper.EntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntityService {
    @Autowired
    private EntityMapper entityMapper;

    @Transactional
    public void insert(Entity entity) {
        entityMapper.insert(entity);
    }
}
