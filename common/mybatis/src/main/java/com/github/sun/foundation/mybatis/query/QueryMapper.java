package com.github.sun.foundation.mybatis.query;

import com.github.sun.foundation.modelling.Model;
import com.github.sun.foundation.sql.DBType;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryMapper<T> {
  @SelectProvider(type = Provider.class, method = "findAll")
  List<T> findAll();

  @SelectProvider(type = Provider.class, method = "findById")
  T findById(@Param("id") Serializable id);

  @SelectProvider(type = Provider.class, method = "findByIds")
  List<T> findByIds(@Param("ids") Set<? extends Serializable> ids);

  @SelectProvider(type = Provider.class, method = "count")
  int count();

  class Provider {
    public String findAll(Map<String, Object> params) {
      Class<?> clazz = (Class<?>) params.get("$RESULT_TYPE");
      SqlBuilder.Factory factory = factory(params);
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(clazz).template();
      return reset(params, template);
    }

    public String findById(Map<String, Object> params) {
      Class<?> clazz = (Class<?>) params.get("$RESULT_TYPE");
      SqlBuilder.Factory factory = factory(params);
      SqlBuilder sb = factory.create();
      Object id = params.get("id");
      SqlBuilder.Template template = sb.from(clazz).where(sb.field(id(clazz)).eq(id)).template();
      return reset(params, template);
    }

    @SuppressWarnings("unchecked")
    public String findByIds(Map<String, Object> params) {
      Class<?> clazz = (Class<?>) params.get("$RESULT_TYPE");
      SqlBuilder.Factory factory = factory(params);
      SqlBuilder sb = factory.create();
      Set<Object> ids = (Set<Object>) params.get("ids");
      if (ids.isEmpty()) {
        throw new IllegalArgumentException("ids is empty");
      }
      SqlBuilder.Template template = sb.from(clazz).where(sb.field(id(clazz)).in(ids)).template();
      return reset(params, template);
    }

    public String count(Map<String, Object> params) {
      Class<?> clazz = (Class<?>) params.get("$RESULT_TYPE");
      SqlBuilder.Factory factory = factory(params);
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(clazz).count().template();
      return reset(params, template);
    }

    @SuppressWarnings("Duplicates")
    private String id(Class<?> clazz) {
      Model model = Model.from(clazz);
      List<Model.Property> pks = model.primaryProperties();
      if (pks.isEmpty()) {
        throw new IllegalArgumentException(model.name() + " missing primary keys");
      }
      return pks.get(0).name();
    }

    @SuppressWarnings("Duplicates")
    private SqlBuilder.Factory factory(Map<String, Object> params) {
      DBType dbType = DBType.valueOf(params.get("$DBType").toString());
      switch (dbType) {
        case PG:
          return SqlBuilderFactory.pg();
        case MYSQL:
          return SqlBuilderFactory.mysql();
        default:
          throw new IllegalArgumentException("Unknown db type: '" + DBType.get() + "'");
      }
    }

    @SuppressWarnings("Duplicates")
    private String reset(Map<String, Object> params, SqlBuilder.Template template) {
      params.clear();
      params.putAll(template.parametersAsMap());
      return template.parameterizedSQL();
    }
  }
}
