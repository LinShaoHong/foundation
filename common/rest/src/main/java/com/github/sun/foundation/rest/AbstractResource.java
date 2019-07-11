package com.github.sun.foundation.rest;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractResource {
  protected Response responseOf() {
    return new Response(Response.OK, null);
  }

  protected Response responseOf(int code, String message) {
    return new Response(code, message);
  }

  protected CountResponse responseOf(int count) {
    return new CountResponse(count);
  }

  protected <T> SingleResponse<T> responseOf(T value) {
    return new SingleResponse<>(value);
  }

  protected <T> ListResponse<T> responseOf(List<T> values) {
    return new ListResponse<>(values);
  }

  protected <T> SetResponse<T> responseOf(Set<T> values) {
    return new SetResponse<>(values);
  }

  protected <T> PageResponse<T> responseOf(int total, List<T> values) {
    return new PageResponse<>(total, values);
  }

  @JsonPropertyOrder({"code", "message"})
  public static class Response {
    public static final int OK = 200;

    public final int code;
    public final String message;

    public Response(int code, String message) {
      this.code = code;
      this.message = message;
    }
  }

  @JsonPropertyOrder({"code", "message", "count"})
  public static class CountResponse extends Response {
    public CountResponse(int count) {
      super(OK, null);
      this.count = count;
    }

    public int count;
  }

  interface ValueResponse<A> {
    <B> ValueResponse<B> map(Function<A, B> func);
  }

  @JsonPropertyOrder({"code", "message", "value"})
  public static class SingleResponse<T> extends Response implements ValueResponse<T> {
    public SingleResponse(T value) {
      super(OK, null);
      this.value = value;
    }

    public final T value;

    @Override
    public <B> ValueResponse<B> map(Function<T, B> func) {
      B v = func.apply(value);
      return new SingleResponse<>(v);
    }
  }

  @JsonPropertyOrder({"code", "message", "values"})
  public static class ListResponse<T> extends Response implements ValueResponse<T> {
    public ListResponse(List<T> values) {
      super(OK, null);
      this.values = values;
    }

    public final List<T> values;

    @Override
    public <B> ValueResponse<B> map(Function<T, B> func) {
      List<B> vs = values.stream().map(func).collect(Collectors.toList());
      return new ListResponse<>(vs);
    }
  }

  @JsonPropertyOrder({"code", "message", "values"})
  public static class SetResponse<T> extends Response implements ValueResponse<T> {
    public SetResponse(Set<T> values) {
      super(OK, null);
      this.values = values;
    }

    public final Set<T> values;

    @Override
    public <B> ValueResponse<B> map(Function<T, B> func) {
      Set<B> vs = values.stream().map(func).collect(Collectors.toSet());
      return new SetResponse<>(vs);
    }
  }

  @JsonPropertyOrder({"code", "message", "total", "values"})
  public static class PageResponse<T> extends Response implements ValueResponse<T> {
    public PageResponse(int total, List<T> values) {
      super(OK, null);
      this.total = total;
      this.values = values;
    }

    public final int total;
    public final List<T> values;

    @Override
    public <B> ValueResponse<B> map(Function<T, B> func) {
      List<B> vs = values.stream().map(func).collect(Collectors.toList());
      return new PageResponse<>(total, vs);
    }
  }
}
