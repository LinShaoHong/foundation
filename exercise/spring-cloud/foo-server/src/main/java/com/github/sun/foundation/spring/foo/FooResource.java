package com.github.sun.foundation.spring.foo;

import com.github.sun.foundation.rest.AbstractResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/v1/api")
public class FooResource extends AbstractResource {
    @GET
    @Path("/foo")
    public String bar() {
        return "Hello, bar";
    }
}
