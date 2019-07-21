package com.github.sun.foundation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.SSL;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.ContextResolver;

public class JerseyClient {
  private static Client client;

  public static Client build() {
    if (client == null) {
      synchronized (JerseyClient.class) {
        if (client == null) {
          ClientConfig config = new ClientConfig();
          config.register((ContextResolver<ObjectMapper>) type -> JSON.getMapper());
          client = ClientBuilder.newBuilder()
            .sslContext(SSL.getContext())
            .hostnameVerifier((hostname, session) -> true)
            .withConfig(config)
            .build();
        }
      }
    }
    return client;
  }
}
