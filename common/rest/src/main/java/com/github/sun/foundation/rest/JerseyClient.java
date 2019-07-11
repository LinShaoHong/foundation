package com.github.sun.foundation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sun.foundation.boot.utility.JSON;
import org.glassfish.jersey.client.ClientConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.ContextResolver;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @Author LinSH
 * @Date: 3:45 PM 2019-07-11
 */
public class JerseyClient {
  private static Client client;

  public Client build() {
    if (client == null) {
      synchronized (JerseyClient.class) {
        if (client == null) {
          ClientConfig config = new ClientConfig();
          config.register((ContextResolver<ObjectMapper>) type -> JSON.getMapper());
          client = ClientBuilder.newBuilder()
            .sslContext(getSSLContext())
            .hostnameVerifier((hostname, session) -> true)
            .withConfig(config)
            .build();
        }
      }
    }
    return client;
  }

  private static SSLContext getSSLContext() {
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
