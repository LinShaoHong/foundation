package com.github.sun.foundation.mybatis;

import com.github.sun.foundation.mybatis.command.WriteMapper;
import com.github.sun.foundation.mybatis.query.QueryMapper;
import com.github.sun.foundation.mybatis.query.SqlTemplateMapper;

public interface CompositeMapper<T> extends SqlTemplateMapper<T>, QueryMapper<T>, WriteMapper<T> {
}
