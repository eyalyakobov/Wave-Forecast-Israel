package com.eyal.waveforecast.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Open-Meteo returns hourly data as parallel arrays, not a list of objects:
 *
 * <pre>
 * "hourly": {
 *   "time":        ["2026-08-20T00:00", "2026-08-20T01:00", ...],
 *   "wave_height": [0.4, 0.45, ...],
 *   "wave_period": [4.1, 4.2, ...]
 * }
 * </pre>
 *
 * Every array has the same length and index i refers to the same hour across
 * all of them. The zipping happens in the ingestion service.
 *
 * <p>Any element may be null — the model has gaps, especially at grid edges
 * and further out in time. Every field here is a boxed type on purpose;
 * primitives would blow up on unboxing.
 */
public final class OpenMeteoDtos {

    private OpenMeteoDtos() {
    }

    // ---------------------------------------------------------------- marine

    public record MarineResponse(
            double latitude,
            double longitude,
            @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
            MarineHourly hourly
    ) {
    }

    public record MarineHourly(
            List<String> time,
            @JsonProperty("wave_height") List<Double> waveHeight,
            @JsonProperty("wave_direction") List<Integer> waveDirection,
            @JsonProperty("wave_period") List<Double> wavePeriod,
            @JsonProperty("swell_wave_height") List<Double> swellWaveHeight,
            @JsonProperty("swell_wave_direction") List<Integer> swellWaveDirection,
            @JsonProperty("swell_wave_period") List<Double> swellWavePeriod,
            @JsonProperty("wind_wave_height") List<Double> windWaveHeight,
            @JsonProperty("wind_wave_direction") List<Integer> windWaveDirection,
            @JsonProperty("wind_wave_period") List<Double> windWavePeriod,
            @JsonProperty("sea_surface_temperature") List<Double> seaSurfaceTemperature
    ) {
    }

    // --------------------------------------------------------------- weather

    public record WeatherResponse(
            double latitude,
            double longitude,
            @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
            WeatherHourly hourly,
            WeatherDaily daily
    ) {
    }

    public record WeatherHourly(
            List<String> time,
            @JsonProperty("temperature_2m") List<Double> temperature2m,
            @JsonProperty("apparent_temperature") List<Double> apparentTemperature,
            @JsonProperty("precipitation") List<Double> precipitation,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("pressure_msl") List<Double> pressureMsl,
            @JsonProperty("visibility") List<Double> visibility,
            @JsonProperty("wind_speed_10m") List<Double> windSpeed10m,
            @JsonProperty("wind_direction_10m") List<Integer> windDirection10m,
            @JsonProperty("wind_gusts_10m") List<Double> windGusts10m
    ) {
    }

    public record WeatherDaily(
            List<String> time,
            List<String> sunrise,
            List<String> sunset
    ) {
    }
}
