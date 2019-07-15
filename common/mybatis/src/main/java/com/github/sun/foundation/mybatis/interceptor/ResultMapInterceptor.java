package com.github.sun.foundation.mybatis.interceptor;

import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.boot.utility.Strings;
import com.github.sun.foundation.boot.utility.TypeInfo;
import com.github.sun.foundation.mybatis.handler.JsonHandler;
import com.github.sun.foundation.sql.DBType;
import com.github.sun.foundation.sql.Model;
import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Intercepts({
  @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class ResultMapInterceptor extends BasicInterceptor {
  private static final Cache<String, ResultMap> resultMapCache = new Cache<>();
  private static final Cache<Class<?>, Class<?>> entityClassCache = new Cache<>();

  @Override
  @SuppressWarnings("unchecked")
  public Object intercept(Invocation invocation) throws Throwable {
    try {
      Object target = invocation.getTarget();
      if (target instanceof BaseExecutor) {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        String statementId = ms.getId();
        int i = statementId.lastIndexOf(".");
        String methodName = statementId.substring(i + 1);
        Class<?> mapperClass = classOf(statementId.substring(0, i));
        Method method = Stream.of(mapperClass.getMethods())
          .filter(m -> m.getName().equals(methodName))
          .findAny()
          .orElse(null);
        if (method != null) {
          Object arg = invocation.getArgs()[1];
          Class<?> entityClass = entityClassCache.get(mapperClass, () -> findEntityClass(mapperClass));
          if (arg instanceof Map) {
            String url = ((BaseExecutor) target).getTransaction().getConnection().getMetaData().getURL();
            DBType dbType = DBType.from(url);
            DBType.set(dbType);
            ((Map) arg).put("$DBType", dbType.name());
            ((Map) arg).put("$RESULT_TYPE", entityClass);
          }
          setResultMaps(ms, entityClass, method);
        }
      }
      return invocation.proceed();
    } finally {
      DBType.remove();
    }
  }

  private void setResultMaps(MappedStatement ms, Class<?> entityClass, Method method) {
    Class<?> c = Model.resultType(method.getGenericReturnType());
    Model model = Model.from(c == null ? entityClass : c);
    if (model.hasProperty()) {
      if (!ms.getResultMaps().isEmpty() && !ms.getResultMaps().get(0).getResultMappings().isEmpty()) {
        Class<?> type = ms.getResultMaps().get(0).getType();
        if (type == model.rawClass()) return;
      }
      ResultMap resultMap = resultMapCache.get(ms.getId(), () -> buildResultMap(ms.getConfiguration(), model, null));
      if (resultMap != null) {
        MetaObject metaMappedStatement = getMetaObject(ms);
        metaMappedStatement.setValue("resultMaps", Collections.singletonList(resultMap));
      }
    }
  }

  private ResultMap buildResultMap(Configuration configuration, Model model, String selectAliasPrefix) {
    List<ResultMapping> mappings = model.properties()
      .stream()
      .map(property -> buildResultMapping(configuration, model, property, selectAliasPrefix))
      .collect(Collectors.toList());
    return new ResultMap.Builder(configuration, makeResultMapId(model, selectAliasPrefix), model.rawClass(), mappings).build();
  }

  private String makeResultMapId(Model model, String selectAliasPrefix) {
    return selectAliasPrefix == null ? model.name() : model.name() + ":" + selectAliasPrefix;
  }

  private ResultMapping buildResultMapping(Configuration configuration, Model model, Model.Property property, String selectAliasPrefix) {
    Class<?> typeHandler = property.typeHandler();
    String nestedResultMapId = null;
    if (typeHandler == null) {
      Model fieldModel = property.model();
      if (fieldModel != null && Map.class.isAssignableFrom(fieldModel.rawClass())) {
        typeHandler = JsonHandler.GenericMapHandler.class;
      } else if (fieldModel != null && fieldModel.hasProperty()) {
        nestedResultMapId = getNestedResultMapId(configuration, fieldModel, property.selectAliasPrefix());
      }
    }
    String name = property.name();
    String column = model.column(selectAliasPrefix == null ? name : Strings.joinCamelCase(selectAliasPrefix, name));
    try {
      return new ResultMapping.Builder(configuration, name, column, property.field().getType())
        .nestedResultMapId(nestedResultMapId)
        .typeHandler(typeHandler == null ? null : (TypeHandler<?>) typeHandler.newInstance())
        .build();
    } catch (InstantiationException | IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getNestedResultMapId(Configuration configuration, Model fieldModel, String selectAliasPrefix) {
    ResultMap resultMap = resultMapCache.get(makeResultMapId(fieldModel, selectAliasPrefix), () -> {
      ResultMap rm = buildResultMap(configuration, fieldModel, selectAliasPrefix);
      if (rm != null) {
        configuration.addResultMap(rm);
      }
      return rm;
    });
    return resultMap != null ? resultMap.getId() : null;
  }

  private Class<?> findEntityClass(Class<?> mapperClass) {
    Type[] types = mapperClass.getGenericInterfaces();
    if (types != null && types.length > 0) {
      Type t = types[0];
      if (t instanceof ParameterizedType) {
        Class<?> c = (Class<?>) ((ParameterizedType) t).getRawType();
        List<Type> parameters = TypeInfo.getTypeParameters(mapperClass, c);
        return (Class<?>) parameters.get(0);
      }
    }
    return null;
  }

  private Class<?> classOf(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Object plugin(Object target) {
    if (target instanceof Executor) {
      return Plugin.wrap(target, this);
    } else {
      return target;
    }
  }

  @Override
  public void setProperties(Properties properties) {
  }
}
