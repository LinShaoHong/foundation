package com.github.sun.foundation.spring.bar;

import com.github.sun.foundation.rest.AbstractResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/v1/api")
public class BarResource extends AbstractResource {
    @GET
    @Path("/bar")
    public String bar() {
        return "Hello, foo";
    }
}
