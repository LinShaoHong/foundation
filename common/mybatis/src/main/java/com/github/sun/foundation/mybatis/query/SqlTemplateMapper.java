package com.github.sun.foundation.mybatis.query;

import com.github.sun.foundation.sql.SqlBuilder;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface SqlTemplateMapper<T> {
  @InsertProvider(type = TemplateSqlProvider.class, method = "provide")
  int insertByTemplate(@Param("template") SqlBuilder.Template template);

  @UpdateProvider(type = TemplateSqlProvider.class, method = "provide")
  int updateByTemplate(@Param("template") SqlBuilder.Template template);

  @DeleteProvider(type = TemplateSqlProvider.class, method = "provide")
  int deleteByTemplate(@Param("template") SqlBuilder.Template template);

  @SelectProvider(type = TemplateSqlProvider.class, method = "provide")
  List<T> findByTemplate(@Param("template") SqlBuilder.Template template);

  @SelectProvider(type = TemplateSqlProvider.class, method = "provide")
  T findOneByTemplate(@Param("template") SqlBuilder.Template template);

  @SelectProvider(type = TemplateSqlProvider.class, method = "provide")
  int countByTemplate(@Param("template") SqlBuilder.Template template);

  @SelectProvider(type = TemplateSqlProvider.class, method = "provide")
  Map<String, Object> findOneByTemplateAsMap(@Param("template") SqlBuilder.Template template);

  @SelectProvider(type = TemplateSqlProvider.class, method = "provide")
  List<Map<String, Object>> findByTemplateAsMap(@Param("template") SqlBuilder.Template template);

  class TemplateSqlProvider {
    public String provide(Map<String, Object> params) {
      SqlBuilder.Template template = (SqlBuilder.Template) params.get("template");
      params.clear();
      params.putAll(template.parametersAsMap());
      return template.parameterizedSQL();
    }
  }
}
