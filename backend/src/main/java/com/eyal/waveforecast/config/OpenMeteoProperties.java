package com.eyal.waveforecast.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for the `openmeteo.*` block in application.yml.
 *
 * Records work as @ConfigurationProperties from Spring Boot 3 onward — no
 * setters, no Lombok, immutable by construction.
 */
@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(
        String marineUrl,
        String forecastUrl,
        String timezone,
        int forecastDays,
        long interRequestDelayMs
) {
}