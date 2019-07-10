package com.github.sun.foundation.boot;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author LinSH
 * @Date: 3:47 PM 2019-03-02
 */
public interface ResolvedType extends Annotated {
  default String name() {
    return rawClass().getName();
  }

  String simpleName();

  Type javaType();

  Class<?> rawClass();

  List<ResolvedType> typeArguments();

  List<ResolvedField> fields();

  List<ResolvedMethod> methods();

  interface ResolvedMember extends Annotated {
    String name();

    List<ResolvedType> parameters();

    boolean hasParameters();

    ResolvedType returnType();

    boolean hasReturnType();

    Member member();
  }

  interface ResolvedField extends ResolvedMember {
    Field field();
  }

  interface ResolvedMethod extends ResolvedMember {
    Method method();
  }

  class ResolvedClass implements ResolvedType {
    private final ParameterizedType pt;
    private final Class<?> rawClass;

    private String simpleName;
    private List<ResolvedType> arguments;

    public ResolvedClass(ParameterizedType pt, Class<?> rawClass) {
      this.pt = pt;
      this.rawClass = rawClass;
    }

    @Override
    public String simpleName() {
      if (simpleName == null) {
        List<ResolvedType> args = typeArguments();
        if (args.isEmpty()) {
          simpleName = rawClass.getSimpleName();
        } else {
          StringBuilder sb = new StringBuilder(rawClass.getSimpleName());
          sb.append("<");
          args.forEach(arg -> sb.append(arg.simpleName()).append(", "));
          sb.setLength(sb.length() - 2);
          sb.append(">");
          simpleName = sb.toString();
        }
      }
      return simpleName;
    }

    @Override
    public Type javaType() {
      return pt == null ? rawClass : pt;
    }

    @Override
    public Class<?> rawClass() {
      return rawClass;
    }

    @Override
    public List<ResolvedType> typeArguments() {
      if (arguments == null) {
        if (pt == null) {
          arguments = Collections.emptyList();
        } else {
          Type[] arr = pt.getActualTypeArguments();
          arguments = Stream.of(arr).map(this::resolve).collect(Collectors.toList());
          arguments = Collections.unmodifiableList(arguments);
        }
      }
      return arguments;
    }

    private ResolvedType resolve(Type type) {
      Type type0 = resolveType(type);
      if (type0 == null) {
        return null;
      }
      if (type0 instanceof ParameterizedType) {
        return new ResolvedClass((ParameterizedType) type0,
          (Class<?>) ((ParameterizedType) type0).getRawType());
      } else if (type0 instanceof Class) {
        return new ResolvedClass(null, (Class<?>) type0);
      }
      return null;
    }

    private Type resolveType(Type type) {
      if (type instanceof Class) {
        return type;
      }
      if (type instanceof ParameterizedType) {
        return resolveType((ParameterizedType) type);
      }
      if (type instanceof WildcardType) {
        return Object.class;
      }
      if (type instanceof TypeVariable) {
        return resolveType((TypeVariable) type);
      }
      return null;
    }

    private Type resolveType(ParameterizedType pt) {
      Type type = resolveType(pt.getRawType());
      if (type == null) {
        return null;
      }
      Type[] arr = pt.getActualTypeArguments();
      List<Type> args = Stream.of(arr)
        .map(this::resolveType)
        .collect(Collectors.toList());
      if (args.stream().anyMatch(Objects::isNull)) {
        return null;
      }
      return TypeInfo.makeGenericType((Class<?>) type, args.toArray(new Type[0]));
    }

    private Type resolveType(TypeVariable variable) {
      TypeVariable<?>[] variables = rawClass.getTypeParameters();
      List<ResolvedType> types = typeArguments();
      for (int i = 0; i < variables.length; i++) {
        if (variables[i].getName().equals(variable.getName())) {
          return types.get(i).javaType();
        }
      }
      return null;
    }

    @Override
    public <T extends Annotation> T annotationOf(Class<T> annotationClass) {
      return rawClass.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] annotationsOf(Class<T> annotationClass) {
      return rawClass.getAnnotationsByType(annotationClass);
    }

    private ResolvedClass resolveDeclaring(Class<?> declaringClass) {
      if (declaringClass == rawClass) {
        return this;
      }
      List<Type> args = TypeInfo.getTypeParameters(javaType(), declaringClass);
      ParameterizedType pt = TypeInfo.makeGenericType(declaringClass, args.toArray(new Type[0]));
      return new ResolvedClass(pt, declaringClass);
    }

    @Override
    public List<ResolvedField> fields() {
      return Stream.of(rawClass().getDeclaredFields())
        .map(field -> new FieldImpl(resolveDeclaring(field.getDeclaringClass()), field))
        .collect(Collectors.toList());
    }

    @Override
    public List<ResolvedMethod> methods() {
      return Stream.of(rawClass().getDeclaredMethods())
        .map(method -> new MethodImpl(resolveDeclaring(method.getDeclaringClass()), method))
        .collect(Collectors.toList());
    }

    abstract class MemberImpl<M extends Member> implements ResolvedMember {
      protected final ResolvedClass declaringType;
      protected final M member;

      public MemberImpl(ResolvedClass declaringType, M member) {
        this.declaringType = declaringType;
        this.member = member;
      }

      @Override
      public String name() {
        return member.getName();
      }

      @Override
      public Member member() {
        return member;
      }

      @Override
      public <T extends Annotation> T annotationOf(Class<T> annotationClass) {
        if (member instanceof AnnotatedElement) {
          return ((AnnotatedElement) member).getAnnotation(annotationClass);
        }
        return null;
      }

      @Override
      public <T extends Annotation> T[] annotationsOf(Class<T> annotationClass) {
        if (member instanceof AnnotatedElement) {
          return ((AnnotatedElement) member).getAnnotationsByType(annotationClass);
        }
        return null;
      }
    }

    class FieldImpl extends MemberImpl<Field> implements ResolvedField {
      private ResolvedType type;

      public FieldImpl(ResolvedClass declaringType, Field member) {
        super(declaringType, member);
      }

      @Override
      public List<ResolvedType> parameters() {
        return Collections.emptyList();
      }

      @Override
      public boolean hasParameters() {
        return false;
      }

      @Override
      public ResolvedType returnType() {
        if (type == null) {
          type = declaringType.resolve(member.getGenericType());
        }
        return type;
      }

      @Override
      public boolean hasReturnType() {
        Class<?> c = member.getType();
        return Void.class != c && void.class != c;
      }

      @Override
      public Field field() {
        return member;
      }
    }

    class MethodImpl extends MemberImpl<Method> implements ResolvedMethod {
      private ResolvedType type;
      private List<ResolvedType> typeParameters;

      public MethodImpl(ResolvedClass declaringType, Method member) {
        super(declaringType, member);
      }

      @Override
      public List<ResolvedType> parameters() {
        if (typeParameters == null) {
          typeParameters = Stream.of(member.getGenericParameterTypes())
            .map(declaringType::resolve)
            .collect(Collectors.toList());
          typeParameters = Collections.unmodifiableList(typeParameters);
        }
        return typeParameters;
      }

      @Override
      public boolean hasParameters() {
        return member.getGenericParameterTypes().length != 0;
      }

      @Override
      public ResolvedType returnType() {
        if (type == null) {
          type = declaringType.resolve(member.getGenericReturnType());
        }
        return type;
      }

      @Override
      public boolean hasReturnType() {
        Class<?> c = member.getReturnType();
        return Void.class != c && void.class != c;
      }

      @Override
      public Method method() {
        return member;
      }
    }
  }

  static ResolvedType resolve(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      Type rawType = pt.getRawType();
      if (rawType instanceof Class) {
        return new ResolvedClass(pt, (Class<?>) rawType);
      }
    } else if (type instanceof Class) {
      return new ResolvedClass(null, (Class<?>) type);
    }
    throw new IllegalArgumentException(type.getTypeName() + " can not be resolved");
  }
}
