package com.github.sun.foundation.mybatis.command;

import com.github.sun.foundation.modelling.Model;
import com.github.sun.foundation.mybatis.command.internal.CommandBackend;
import com.github.sun.foundation.sql.DBType;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.UpdateProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface WriteMapper<T> {
  @InsertProvider(type = Provider.class, method = "insertAll")
  int insertAll(@Param("values") List<T> values);

  @InsertProvider(type = Provider.class, method = "insert")
  int insert(@Param("value") T value);

  @InsertProvider(type = Provider.class, method = "replaceAll")
  int replaceAll(@Param("values") List<T> values);

  @InsertProvider(type = Provider.class, method = "replace")
  int replace(@Param("value") T value);

  @UpdateProvider(type = Provider.class, method = "updateAll")
  int updateAll(@Param("values") List<T> values);

  @UpdateProvider(type = Provider.class, method = "update")
  int update(@Param("value") T value);

  @DeleteProvider(type = Provider.class, method = "deleteAll")
  int deleteAll(@Param("values") List<T> values);

  @DeleteProvider(type = Provider.class, method = "delete")
  int delete(@Param("value") T value);

  @DeleteProvider(type = Provider.class, method = "deleteById")
  int deleteById(@Param("id") Serializable id);

  @DeleteProvider(type = Provider.class, method = "deleteByIds")
  int deleteByIds(@Param("ids") List<? extends Serializable> ids);

  @SuppressWarnings("unchecked")
  class Provider {
    public String insertAll(Map<String, Object> params) {
      List<Object> values = (List<Object>) params.get("values");
      SqlBuilder.Template template = getRunner(params).insert(values);
      return reset(params, template);
    }

    public String insert(Map<String, Object> params) {
      Object value = params.get("value");
      SqlBuilder.Template template = getRunner(params).insert(value);
      return reset(params, template);
    }

    public String replaceAll(Map<String, Object> params) {
      List<Object> values = (List<Object>) params.get("values");
      SqlBuilder.Template template = getRunner(params).replace(values);
      return reset(params, template);
    }

    public String replace(Map<String, Object> params) {
      Object value = params.get("value");
      SqlBuilder.Template template = getRunner(params).replace(value);
      return reset(params, template);
    }

    public String updateAll(Map<String, Object> params) {
      List<Object> values = (List<Object>) params.get("values");
      SqlBuilder.Template template = getRunner(params).update(values);
      return reset(params, template);
    }

    public String update(Map<String, Object> params) {
      Object value = params.get("value");
      SqlBuilder.Template template = getRunner(params).update(value);
      return reset(params, template);
    }

    public String deleteAll(Map<String, Object> params) {
      List<Object> values = (List<Object>) params.get("values");
      SqlBuilder.Template template = getRunner(params).delete(values);
      return reset(params, template);
    }

    public String delete(Map<String, Object> params) {
      Object value = params.get("value");
      SqlBuilder.Template template = getRunner(params).delete(value);
      return reset(params, template);
    }

    public String deleteById(Map<String, Object> params) {
      Object id = params.get("id");
      params.remove("id");
      params.put("ids", Collections.singletonList(id));
      return deleteByIds(params);
    }

    public String deleteByIds(Map<String, Object> params) {
      List<Object> ids = (List<Object>) params.get("ids");
      if (ids.isEmpty()) {
        throw new IllegalArgumentException("ids is empty");
      }
      Class<?> clazz = (Class<?>) params.get("$RESULT_TYPE");
      String id = id(clazz);
      SqlBuilder.Factory factory = factory(params);
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(clazz)
        .where(ids.size() == 1 ? sb.field(id).eq(ids.get(0)) : sb.field(id).in(ids))
        .delete()
        .template();
      return reset(params, template);
    }

    private CommandRunner<Object> getRunner(Map<String, Object> params) {
      return new CommandBackend<>(factory(params));
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
    private String id(Class<?> clazz) {
      Model model = Model.from(clazz);
      List<Model.Property> pks = model.primaryProperties();
      if (pks.isEmpty()) {
        throw new IllegalArgumentException(model.name() + " missing primary keys");
      }
      return pks.get(0).name();
    }

    @SuppressWarnings("Duplicates")
    private String reset(Map<String, Object> params, SqlBuilder.Template template) {
      params.clear();
      params.putAll(template.parametersAsMap());
      return template.parameterizedSQL();
    }
  }
}
