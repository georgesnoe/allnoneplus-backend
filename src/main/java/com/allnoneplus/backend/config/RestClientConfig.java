package com.allnoneplus.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the planning API.
 * The base URL and an optional cookie are driven by environment
 * variables (see PlanningProperties).
 */
@Configuration
public class RestClientConfig {

  @Bean
  public RestClient planningRestClient(PlanningProperties properties) {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("X-Requested-With", "XMLHttpRequest");
    if (properties.cookie() != null && !properties.cookie().isBlank()) {
      builder.defaultHeader(HttpHeaders.COOKIE, properties.cookie());
    }
    return builder.build();
  }
}
