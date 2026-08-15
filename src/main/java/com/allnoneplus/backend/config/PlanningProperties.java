package com.allnoneplus.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Planning configuration (see application.yaml).
 */
@ConfigurationProperties(prefix = "app.planning")
public record PlanningProperties(
    String baseUrl,
    String cookie) {
}
