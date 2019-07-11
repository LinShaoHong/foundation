package com.github.sun.foundation.boot.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 响应异常, 自定义异常需继承ResponsiveException，用以将异常信息返回给前端
 */
public abstract class ResponsiveException extends RuntimeException {
  /**
   * 异常类型
   */
  public enum Kind {
    /**
     * 参数错误
     */
    BAD_REQUEST,
    /**
     * 资源不存在
     */
    NOT_FOUND,
    /**
     * 违反业务约束
     */
    ANTI_CONSTRAINT,
    /**
     * 权限不足
     */
    ACCESS_DENIED,
    /**
     * 未登陆
     */
    UNAUTHORIZED,
    /**
     * 超时
     */
    TIMEOUT,
    /**
     * 服务器出错
     */
    SERVER_ERROR,
    /**
     * 不可预期的错误
     */
    UNEXPECTED
  }

  private final Kind kind;

  ResponsiveException(String message, Kind kind) {
    super(message);
    this.kind = kind;
  }

  ResponsiveException(String message, Kind kind, Throwable cause) {
    super(message, cause);
    this.kind = kind;
  }

  public Kind getKind() {
    return kind;
  }

  public static String stackTraceOf(Throwable ex) {
    try (StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      return sw.toString();
    } catch (IOException ex2) {
      return ex.getMessage();
    }
  }
}
