package com.github.sun.foundation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.client.ClientProperties;

@UtilityClass
public class Locations {
  public String fromIp(String ip) {
    String location = null;
    if (ip != null && !ip.isEmpty()) {
      try {
        JsonNode node = JerseyClient.of()
          .property(ClientProperties.READ_TIMEOUT, 3000)
          .property(ClientProperties.CONNECT_TIMEOUT, 3000)
          .target("http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=status,country,city,regionName")
          .request()
          .get()
          .readEntity(JsonNode.class);
        String status = node.get("status").asText();
        if ("success".equals(status)) {
          String city = node.get("city").asText();
          String regionName = node.get("regionName").asText();
          String country = node.get("country").asText();
          location = country + ":" + regionName + ":" + city;
        }
      } catch (Throwable ex) {
        // do nothing
      }
    }
    return location;
  }
}
