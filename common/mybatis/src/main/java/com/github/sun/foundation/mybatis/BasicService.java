package com.github.sun.foundation.mybatis;

import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class BasicService<K extends Serializable, V, M extends CompositeMapper<V>> {
    @Resource
    protected M mapper;

    @Transactional
    public int insertAll(Iterable<V> values) {
        return mapper.insertAll(Iterators.asList(values));
    }

    @Transactional
    public int insert(V value) {
        return mapper.insert(value);
    }

    @Transactional
    public int updateAll(Iterable<V> values) {
        return mapper.updateAll(Iterators.asList(values));
    }

    @Transactional
    public int update(V value) {
        return mapper.update(value);
    }

    @Transactional
    public int deleteAll(Iterable<V> values) {
        return mapper.deleteAll(Iterators.asList(values));
    }

    @Transactional
    public int delete(V value) {
        return mapper.delete(value);
    }

    @Transactional
    public int deleteById(K id) {
        return mapper.deleteById(id);
    }

    @Transactional
    public int deleteByIds(List<K> ids) {
        return mapper.deleteByIds(ids);
    }

    @Transactional
    public int insertByTemplate(SqlBuilder.Template template) {
        return 0;
    }

    @Transactional
    public int updateByTemplate(SqlBuilder.Template template) {
        return 0;
    }

    @Transactional
    public int deleteByTemplate(SqlBuilder.Template template) {
        return 0;
    }

    public List<V> findAll() {
        return mapper.findAll();
    }

    public V findById(K id) {
        return mapper.findById(id);
    }

    public List<V> findByIds(Iterable<K> ids) {
        return mapper.findByIds(new HashSet<>(Iterators.asCollection(ids)));
    }

    public int count() {
        return mapper.count();
    }

    public List<V> findByTemplate(SqlBuilder.Template template) {
        return mapper.findByTemplate(template);
    }

    public V findOneByTemplate(SqlBuilder.Template template) {
        return mapper.findOneByTemplate(template);
    }

    public int countByTemplate(SqlBuilder.Template template) {
        return mapper.countByTemplate(template);
    }

    public List<Map<String, Object>> findByTemplateAsMap(SqlBuilder.Template template) {
        return mapper.findByTemplateAsMap(template);
    }

    public Map<String, Object> findOneByTemplateAsMap(SqlBuilder.Template template) {
        return mapper.findOneByTemplateAsMap(template);
    }
}
