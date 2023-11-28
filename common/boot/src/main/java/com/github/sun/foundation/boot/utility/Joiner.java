package com.github.sun.foundation.boot.utility;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.sun.foundation.boot.utility.Reflections.*;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数据填充工具类
 */
public class Joiner {
  private final static String[] PKS = Scanner.class.getPackage().getName().split("\\.");
  private static final Map<Class<?>, List<Property>> props = new ConcurrentHashMap<>();
  private static final ThreadLocal<Map<String, Object>> params = new ThreadLocal<>();
  private static final com.github.benmanes.caffeine.cache.Cache<String, Object> cache =
    Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

  public static <T> T join(T value) {
    if (value == null) {
      return null;
    }
    return join(Collections.singletonList(value)).get(0);
  }

  public static <T> List<T> join(List<T> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    Class<?> clazz = values.get(0).getClass();
    List<Property> properties = props.get(clazz);
    if (properties == null) {
      properties = parseProps(clazz);
      props.put(clazz, properties);
    }
    if (!properties.isEmpty()) {
      Map<Boolean, List<Property>> map = properties.stream().collect(Collectors.groupingBy(Property::isIdRef));
      List<Property> idProps = map.get(true);
      List<Property> geProps = map.get(false);
      if (idProps != null && !idProps.isEmpty()) {
        joinId(idProps, values);
      }
      if (geProps != null && !geProps.isEmpty()) {
        joinGe(geProps, values);
      }
    }
    List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toList());
    fields.removeIf(f -> f.getType().isPrimitive() ||
      f.getType().isEnum() ||
      Map.class.isAssignableFrom(f.getType()) ||
      !List.class.isAssignableFrom(f.getType()) ||
      f.getType().getPackage() == null ||
      !f.getType().getPackage().getName().startsWith(PKS[0] + "." + PKS[1]));
    if (!fields.isEmpty()) {
      fields.forEach(field -> {
        boolean isList = List.class.isAssignableFrom(field.getType());
        List<Object> subValues = new ArrayList<>();
        values.forEach(value -> {
          Object subValue = getValue(value, field);
          if (subValue != null) {
            if (isList) {
              subValues.addAll(((List<?>) subValue));
            } else {
              subValues.add(subValue);
            }
          }
        });
        join(subValues);
      });
    }
    return values;
  }

  public static void put(String name, Object value) {
    Map<String, Object> map = params.get();
    if (map == null) {
      map = new HashMap<>();
    }
    map.put(name, value);
    params.set(map);
  }

  @Data
  @AllArgsConstructor
  static class ArgsHolder {
    private Property p;
    private Object[] args;

    public static ArgsHolder from(Property p, Object[] args) {
      return new ArgsHolder(p, args);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ArgsHolder ah = (ArgsHolder) o;
      if (!Objects.equals(p.methodKey(), ah.getP().methodKey())) {
        return false;
      }
      for (int i = 0; i < ah.getArgs().length - 1; i++) {
        if (!Objects.equals(ah.getArgs()[i], this.args[i])) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      if (args.length == 1) {
        return Objects.hashCode(p.methodKey());
      } else {
        return Objects.hash(p.methodKey(), Arrays.hashCode(Arrays.copyOfRange(args, 0, args.length - 1)));
      }
    }
  }

  /**
   * id属性分批获取
   */
  @SuppressWarnings("unchecked")
  private static <T> void joinId(List<Property> idProps, List<T> values) {
    idProps.sort(Comparator.comparingInt(Property::getOrder));
    Map<Integer, List<Property>> orderGroup = new LinkedHashMap<>();
    idProps.forEach(p -> orderGroup.computeIfAbsent(p.getOrder(), r -> new ArrayList<>()).add(p));

    orderGroup.forEach((ok, ov) -> {
      Map<String, Map<String, Object>> refValues = new HashMap<>();
      Map<String, List<Property>> groupedProps = ov.stream().collect(Collectors.groupingBy(Property::methodKey));
      groupedProps.forEach((key, props) -> {
        Map<ArgsHolder, List<ArgsHolder>> groupedValues = new HashMap<>();
        props.forEach(p -> values.forEach(v -> {
          ArgsHolder holder = ArgsHolder.from(p, parseArgs(p, v));
          groupedValues.computeIfAbsent(holder, h -> new ArrayList<>()).add(holder);
        }));
        Property p = props.get(0);
        groupedValues.forEach((ps, vs) -> {
          Set<Object> ids = new HashSet<>();
          vs.forEach(v -> {
            Object idValue = v.getArgs()[v.getArgs().length - 1];
            if (idValue != null) {
              if (idValue instanceof String) {
                for (String id : ((String) idValue).split(",")) {
                  ids.add(Long.parseLong(id));
                }
              } else if (idValue instanceof Collection) {
                ids.addAll((Collection<?>) idValue);
              } else {
                ids.add(idValue);
              }
            }
          });
          if (!ids.isEmpty()) {
            Map<String, Object> map;
            if (p.isCache()) {
              List<String> keys = ids.stream().map(p::argKey).collect(Collectors.toList());
              map = cache.getAll(keys, ks -> {
                Set<Object> _ids = new HashSet<>();
                ks.forEach(k -> _ids.add(Long.parseLong(p.keyToArg(k))));
                ps.args[ps.args.length - 1] = _ids;
                Collection<Object> list = (Collection<Object>) invoke(p.getStub(), p.method, ps.args);
                Map<String, Object> _map = new HashMap<>();
                if (!list.isEmpty()) {
                  Field f = getField(list.iterator().next().getClass(), p.getIdName());
                  _map = list.stream().collect(Collectors.toMap(_v -> p.argKey(getValue(_v, f)), _v -> _v, (m1, m2) -> m1));
                }
                return _map;
              });
            } else {
              ps.args[ps.args.length - 1] = ids;
              Collection<Object> list = (Collection<Object>) invoke(p.getStub(), p.method, ps.args);
              map = new HashMap<>();
              if (!list.isEmpty()) {
                Field f = getField(list.iterator().next().getClass(), p.getIdName());
                map = list.stream().collect(Collectors.toMap(_v -> p.argKey(getValue(_v, f)), _v -> _v, (m1, m2) -> m1));
              }
            }
            Map<String, Object> _map = refValues.get(p.methodKey());
            if (_map != null) {
              map.putAll(_map);
            }
            refValues.put(p.methodKey(), map);
          }
        });
      });

      ov.forEach(p -> values.forEach(v -> {
        Object[] args = parseArgs(p, v);
        Object idValue = args[args.length - 1];
        if (idValue != null) {
          Map<String, Object> map = refValues.get(p.methodKey());
          if (map != null && !map.isEmpty()) {
            //获取值
            Object value;
            if (idValue instanceof String) {
              value = new ArrayList<>();
              for (String id : ((String) idValue).split(",")) {
                Object _value = map.get(p.argKey(Long.parseLong(id)));
                if (_value != null) {
                  ((Collection<Object>) value).add(_value);
                }
              }
            } else if (idValue instanceof Collection) {
              value = new ArrayList<>();
              for (Object id : ((Collection<?>) idValue)) {
                Object _value = map.get(p.argKey(id));
                if (_value != null) {
                  ((Collection<Object>) value).add(_value);
                }
              }

            } else {
              value = map.get(p.argKey(idValue));
            }

            //解析值
            if (value != null) {
              if (p.retHandler.getClass() != Ref.RetDefault.class) {
                value = p.getRetHandler().apply(value);
              } else if (!StringUtils.isEmpty(p.getSelect())) {
                String select = p.getSelect();
                if (value instanceof Collection) {
                  value = ((Collection<?>) value).stream()
                    .map(_v -> getValue(_v, select))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                } else {
                  value = getValue(value, select);
                }
              }
              //设置值
              setValue(v, p.field, value);
            }
          }
        }
      }));
    });
  }

  /**
   * 一般属性逐个获取
   */
  private static <T> void joinGe(List<Property> geProps, List<T> values) {
    geProps.forEach(p -> values.forEach(v -> {
      Object[] args = parseArgs(p, v);
      if (Arrays.stream(args).allMatch(Objects::nonNull)) {
        Object result;
        if (p.isCache()) {
          result = cache.get(p.argsKey(args), k -> invoke(p.getStub(), p.getMethod(), args));
        } else {
          result = invoke(p.getStub(), p.getMethod(), args);
        }
        if (result != null) {
          if (p.retHandler.getClass() != Ref.RetDefault.class) {
            result = p.getRetHandler().apply(result);
          } else if (!StringUtils.isEmpty(p.getSelect())) {
            result = getValue(result, p.getSelect());
          }
          setValue(v, p.field, result);
        }
      }
    }));
  }

  private static Object[] parseArgs(Property p, Object obj) {
    if (p.getArgHandler().getClass() != Ref.ArgDefault.class) {
      return p.argHandler.apply(obj);
    } else {
      Object[] args = new Object[p.getArgs().size()];
      for (int i = 0; i < args.length; i++) {
        Object arg = null;
        String name = p.getArgs().get(i);
        if (name.startsWith("{") && name.endsWith("}")) {
          name = name.substring(1, name.length() - 1);
          if (params.get() != null) {
            arg = params.get().get(name);
          }
        } else if (name.startsWith("code(") && name.endsWith(")")) {
          name = name.substring(5, name.length() - 1);
          if (name.startsWith("'") && name.endsWith("'")) {
            arg = name.substring(1, name.length() - 1);
          } else if ("true".equals(name)) {
            arg = true;
          } else if ("false".equals(name)) {
            arg = false;
          } else {
            arg = Integer.valueOf(name);
          }
        } else {
          Field field = p.getField(name);
          if (field != null) {
            arg = getValue(obj, field);
          }
        }
        args[i] = arg;
      }
      return args;
    }
  }

  private static List<Property> parseProps(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    return Arrays.stream(fields).filter(v -> v.isAnnotationPresent(Ref.class))
      .map(field -> {
        Ref ref = field.getAnnotation(Ref.class);
        Method method = getMethod(ref.stub(), ref.method());
        if (method == null) {
          throw new IllegalArgumentException(ref.stub().getSimpleName() + "找不到方法" + ref.method());
        }
        return Property.builder()
          .clazz(clazz)
          .field(field)
          .args(Arrays.asList(ref.args().replace(" ", "").split(",")))
          .stub(newInstance(ref.stub()))
          .idRef(ref.idRef())
          .idName(ref.idName())
          .cache(ref.cache())
          .select(ref.select().replaceAll(" ", ""))
          .method(method)
          .order(ref.order())
          .argHandler((Ref.ArgHandler) newInstance(ref.argHandler()))
          .retHandler((Ref.RetHandler) newInstance(ref.retHandler()))
          .build();
      }).collect(Collectors.toList());
  }

  @Data
  @Builder
  private static class Property {
    private Class<?> clazz;
    private Field field;
    private List<String> args;
    private Object stub;
    private boolean idRef;
    private String idName;
    private boolean cache;
    private int order;
    private String select;
    private Method method;
    private Ref.ArgHandler argHandler;
    private Ref.RetHandler retHandler;

    public Field getField(String name) {
      return Reflections.getField(clazz, name);
    }

    public String methodKey() {
      return stub.getClass().getSimpleName() + ":" + method.getName();
    }

    public String argKey(Object arg) {
      return methodKey() + "(" + (arg == null ? "null" : arg.toString()) + ")";
    }

    public String argsKey(Object[] args) {
      String params = Stream.of(args).map(v -> v == null ? "null" : v.toString()).collect(Collectors.joining(","));
      return methodKey() + "(" + params + ")";
    }

    public String keyToArg(String key) {
      return key.substring(methodKey().length() + 1, key.length() - 1);
    }
  }

  /**
   * 标记数据填充规则
   */
  @Documented
  @Target({FIELD})
  @Retention(RUNTIME)
  public @interface Ref {
    /**
     * 数据获取接口端
     */
    Class<?> stub();

    /**
     * 调用stub的方法获取数据
     */
    String method() default "listByIds";

    /**
     * 指明方法调用的入参
     * 如果形如#{name}，则对应值为Models.put(name,value)进行设置的
     * 否则，对应值为被填充对象对应属性名的值
     * 多个用","隔开
     */
    String args() default "";

    /**
     * 指明选取返回对象中对应属性的值进行填充
     */
    String select() default "name";

    /**
     * 指明入参是否是引用对象的id
     */
    boolean idRef() default true;

    /**
     * 被Join对象的id字段名
     */
    String idName() default "id";

    /**
     * 是否使用缓存机制，提高数据缓存的效率
     */
    boolean cache() default false;

    /**
     * 在joinId的过程中，先对order进行分组，值越大，越往后执行
     */
    int order() default 0;

    /**
     * stub.method()的入参处理器
     * 如果为ArgDefault，则使用args()获取入参
     */
    Class<? extends ArgHandler> argHandler() default ArgDefault.class;

    /**
     * 自定义stub.method()返回值处理器，获取正确的填充值
     * 如果为Default，则使用select()属性进行填充
     */
    Class<? extends RetHandler> retHandler() default RetDefault.class;

    interface ArgHandler {
      /**
       * @param value 填充对象
       * @return 参数列表
       */
      Object[] apply(Object value);
    }

    interface RetHandler {
      /**
       * @param value stub.method()返回值
       * @return 填充数据
       */
      Object apply(Object value);
    }

    class ArgDefault implements ArgHandler {
      @Override
      public Object[] apply(Object value) {
        return new Object[]{null};
      }
    }

    class RetDefault implements RetHandler {
      @Override
      public Object apply(Object value) {
        return value;
      }
    }
  }
}