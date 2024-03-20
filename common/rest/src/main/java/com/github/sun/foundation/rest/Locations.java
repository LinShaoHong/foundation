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
          .target("https://webapi-pc.meitu.com/common/ip_location?ip=" + ip)
          .request()
          .get()
          .readEntity(JsonNode.class);
        int code = node.get("code").asInt();
        if (code == 0) {
          String city = node.get("data").get(ip).get("city").asText();
          String province = node.get("data").get(ip).get("province").asText();
          String nation = node.get("data").get(ip).get("nation").asText();
          location = nation + ":" + province + ":" + city;
        }
      } catch (Throwable ex) {
        // do nothing
      }
    }
    return location;
  }
}