package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import javax.ws.rs.container.ContainerRequestContext;

@UtilityClass
public class IPs {
  public String getRemoteIP(ContainerRequestContext ctx) {
    String ip;
    try {
      ip = ctx.getHeaderString("x-forwarded-for");
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
        ip = ctx.getHeaderString("Proxy-Client-IP");
      }
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
        ip = ctx.getHeaderString("WL-Proxy-Client-IP");
      }
      // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
      if (ip != null && ip.length() > 15) { // "***.***.***.***".length()
        if (ip.indexOf(",") > 0) {
          ip = ip.substring(0, ip.indexOf(","));
        }
      }
    } catch (Exception e) {
      ip = "";
    }
    return ip;
  }
}
