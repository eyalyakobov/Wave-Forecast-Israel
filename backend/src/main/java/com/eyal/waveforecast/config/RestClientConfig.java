package com.eyal.waveforecast.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OpenMeteoProperties.class)
public class RestClientConfig {

    /**
     * Explicit timeouts matter. The default request factory waits forever, so
     * one hung connection to Open-Meteo would stall the scheduled ingestion
     * run indefinitely and you'd never see it in the logs.
     */
    @Bean
    public RestClient restClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "WaveForecastIsrael/1.0")
                .build();
    }
}
