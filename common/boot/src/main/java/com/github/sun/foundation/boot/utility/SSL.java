package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@UtilityClass
public class SSL {
  public SSLContext getContext() {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    }};

    // Install the all-trusting trust manager
    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLSv1.2");
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
    try {
      sslContext.init(null, trustAllCerts, new SecureRandom());
    } catch (KeyManagementException ex) {
      throw new RuntimeException(ex);
    }
    return sslContext;
  }
}
