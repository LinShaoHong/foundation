package com.github.sun.foundation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sun.foundation.boot.InjectionProvider;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.SSL;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.ContextResolver;

public class JerseyClient {
    private static volatile Client client;

    public static Client of() {
        return Injector.getInstance(Client.class);
    }

    private static Client build() {
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

    public static class Provider implements InjectionProvider {
        @Override
        public void config(Binder binder) {
            binder.named("rest-client").bind(build());
        }
    }
}