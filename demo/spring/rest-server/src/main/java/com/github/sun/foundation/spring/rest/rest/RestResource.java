package com.github.sun.foundation.spring.rest.rest;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.spring.rest.entity.Entity;
import com.github.sun.foundation.spring.rest.mapper.EntityMapper;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.foundation.sql.factory.SqlBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/api")
public class RestResource extends AbstractResource {

  @Autowired
  private EntityMapper mapper;

  @GET
  @Path("/test")
  @Produces(MediaType.APPLICATION_JSON)
  public SingleResponse<Entity> test() {
    SqlBuilder.Factory factory = SqlBuilderFactory.mysql();
    SqlBuilder sb = factory.create();
    return responseOf(mapper.findOneByTemplate(sb.from(Entity.class).where(sb.field("id").eq("a")).template()));
  }
}
