package com.eyal.waveforecast.client;

import com.eyal.waveforecast.client.dto.OpenMeteoDtos.MarineResponse;
import com.eyal.waveforecast.client.dto.OpenMeteoDtos.WeatherResponse;
import com.eyal.waveforecast.config.OpenMeteoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Thin HTTP client over the two Open-Meteo endpoints we use.
 *
 * <p>No API key: Open-Meteo is free for non-commercial use under CC BY 4.0.
 * Attribution belongs in the frontend footer — don't skip it.
 *
 * <p>This class does transport only. Parsing the parallel arrays into rows is
 * the ingestion service's job; keeping that split means the client can be
 * tested against recorded JSON without touching the database.
 */
@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private static final String MARINE_HOURLY = String.join(",",
            "wave_height", "wave_direction", "wave_period",
            "swell_wave_height", "swell_wave_direction", "swell_wave_period",
            "wind_wave_height", "wind_wave_direction", "wind_wave_period",
            "sea_surface_temperature");

    private static final String WEATHER_HOURLY = String.join(",",
            "temperature_2m", "apparent_temperature", "precipitation",
            "weather_code", "pressure_msl", "visibility",
            "wind_speed_10m", "wind_direction_10m", "wind_gusts_10m");

    private static final String WEATHER_DAILY = "sunrise,sunset";

    private final RestClient restClient;
    private final OpenMeteoProperties props;

    public OpenMeteoClient(RestClient restClient, OpenMeteoProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    /**
     * Wave and sea data. Pass the spot's <em>marine</em> point, not the shore
     * point — land coordinates return nulls across the board.
     */
    public MarineResponse fetchMarine(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(props.marineUrl())
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", MARINE_HOURLY)
                .queryParam("timezone", props.timezone())
                .queryParam("forecast_days", props.forecastDays())
                .build()
                .toUri();

        log.debug("GET marine {},{}", latitude, longitude);
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(MarineResponse.class);
    }

    /**
     * Atmospheric data. The shore point is fine (and slightly better) here —
     * wind over the beach is what a surfer actually stands in.
     */
    public WeatherResponse fetchWeather(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString(props.forecastUrl())
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", WEATHER_HOURLY)
                .queryParam("daily", WEATHER_DAILY)
                .queryParam("timezone", props.timezone())
                .queryParam("forecast_days", props.forecastDays())
                .build()
                .toUri();

        log.debug("GET weather {},{}", latitude, longitude);
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(WeatherResponse.class);
    }
}
