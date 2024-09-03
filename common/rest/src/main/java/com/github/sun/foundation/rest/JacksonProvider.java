package com.github.sun.foundation.rest;

import com.github.sun.foundation.boot.utility.JSON;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.ws.rs.ext.Provider;

@Provider
public class JacksonProvider extends JacksonJaxbJsonProvider {
    public JacksonProvider() {
        super.setMapper(JSON.getMapper());
    }
}
