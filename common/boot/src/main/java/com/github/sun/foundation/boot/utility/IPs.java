package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@UtilityClass
public class IPs {
  public String getRemoteIP(HttpServletRequest request) {
    String ip = request.getHeader("X-Real-IP");
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Forwarded-For");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
