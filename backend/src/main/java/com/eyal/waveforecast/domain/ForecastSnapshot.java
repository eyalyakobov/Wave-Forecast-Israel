package com.eyal.waveforecast.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One row per (spot, forecast hour).
 *
 * <p>This class does double duty, which is worth understanding:
 *
 * <ul>
 *   <li><b>As a JPA entity</b> — read path. Query methods on
 *       {@code ForecastSnapshotRepository} load these normally.</li>
 *   <li><b>As a plain carrier</b> — write path. {@code ForecastIngestionService}
 *       builds unmanaged instances and hands them to
 *       {@code ForecastSnapshotBatchImpl}, which writes them via JdbcTemplate.
 *       Those instances are never attached to a persistence context and their
 *       {@code id} stays null.</li>
 * </ul>
 *
 * <p>That split is deliberate (see CLAUDE.md, decision 3). The practical
 * consequence: don't add JPA lifecycle callbacks such as {@code @PrePersist}
 * here expecting them to fire during ingestion. They won't — the batch path
 * bypasses Hibernate entirely.
 *
 * <p>Every numeric field is boxed. The forecast models have gaps, especially
 * at grid edges and further out in time, and a primitive would throw on
 * unboxing a null.
 */
@Entity
@Table(
        name = "forecast_snapshot",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_snapshot_spot_time",
                columnNames = {"spot_id", "forecast_time"}
        )
)
public class ForecastSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * LAZY on purpose. The common read is "give me 168 hours for one spot" —
     * eager loading would re-fetch the same Spot 168 times.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(name = "forecast_time", nullable = false)
    private Instant forecastTime;

    /*
     * insertable/updatable false: the database owns this via `now()`, both in
     * the DEFAULT and in the ON CONFLICT branch of the batch upsert.
     */
    @Column(name = "fetched_at", nullable = false, insertable = false, updatable = false)
    private Instant fetchedAt;

    /**
     * Which model most likely produced this row. Inferred from the 3-day
     * boundary, not reported by Open-Meteo — see CLAUDE.md, decision 5.
     * Drives the UI confidence indicator; never shown to users as fact.
     */
    @Column(name = "model_source", length = 40)
    private String modelSource;

    // ------------------------------------------------------- combined wave

    @Column(name = "wave_height")
    private Double waveHeight;

    @Column(name = "wave_direction")
    private Integer waveDirection;

    @Column(name = "wave_period")
    private Double wavePeriod;

    // ------------------------------------------------------------- swell
    // Long-period energy from distant systems. This is what surfers care
    // about; it is not interchangeable with waveHeight.

    @Column(name = "swell_wave_height")
    private Double swellWaveHeight;

    @Column(name = "swell_wave_direction")
    private Integer swellWaveDirection;

    @Column(name = "swell_wave_period")
    private Double swellWavePeriod;

    // --------------------------------------------------------- wind wave
    // Locally generated chop. High wind wave with low swell means messy
    // conditions even when the combined height looks appealing.

    @Column(name = "wind_wave_height")
    private Double windWaveHeight;

    @Column(name = "wind_wave_direction")
    private Integer windWaveDirection;

    @Column(name = "wind_wave_period")
    private Double windWavePeriod;

    @Column(name = "sea_surface_temperature")
    private Double seaSurfaceTemperature;

    // ------------------------------------------------------- atmospheric

    @Column(name = "temperature_2m")
    private Double temperature2m;

    @Column(name = "apparent_temperature")
    private Double apparentTemperature;

    @Column(name = "precipitation")
    private Double precipitation;

    /** WMO weather interpretation code. Maps to icon + Hebrew label in the UI. */
    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "pressure_msl")
    private Double pressureMsl;

    /** Metres. */
    @Column(name = "visibility")
    private Double visibility;

    @Column(name = "wind_speed_10m")
    private Double windSpeed10m;

    @Column(name = "wind_direction_10m")
    private Integer windDirection10m;

    @Column(name = "wind_gusts_10m")
    private Double windGusts10m;

    // ------------------------------------------------------------ getters

    public Long getId() {
        return id;
    }

    public Spot getSpot() {
        return spot;
    }

    public Instant getForecastTime() {
        return forecastTime;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getModelSource() {
        return modelSource;
    }

    public Double getWaveHeight() {
        return waveHeight;
    }

    public Integer getWaveDirection() {
        return waveDirection;
    }

    public Double getWavePeriod() {
        return wavePeriod;
    }

    public Double getSwellWaveHeight() {
        return swellWaveHeight;
    }

    public Integer getSwellWaveDirection() {
        return swellWaveDirection;
    }

    public Double getSwellWavePeriod() {
        return swellWavePeriod;
    }

    public Double getWindWaveHeight() {
        return windWaveHeight;
    }

    public Integer getWindWaveDirection() {
        return windWaveDirection;
    }

    public Double getWindWavePeriod() {
        return windWavePeriod;
    }

    public Double getSeaSurfaceTemperature() {
        return seaSurfaceTemperature;
    }

    public Double getTemperature2m() {
        return temperature2m;
    }

    public Double getApparentTemperature() {
        return apparentTemperature;
    }

    public Double getPrecipitation() {
        return precipitation;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public Double getPressureMsl() {
        return pressureMsl;
    }

    public Double getVisibility() {
        return visibility;
    }

    public Double getWindSpeed10m() {
        return windSpeed10m;
    }

    public Integer getWindDirection10m() {
        return windDirection10m;
    }

    public Double getWindGusts10m() {
        return windGusts10m;
    }

    // ------------------------------------------------------------ setters

    public void setSpot(Spot spot) {
        this.spot = spot;
    }

    public void setForecastTime(Instant forecastTime) {
        this.forecastTime = forecastTime;
    }

    public void setModelSource(String modelSource) {
        this.modelSource = modelSource;
    }

    public void setWaveHeight(Double waveHeight) {
        this.waveHeight = waveHeight;
    }

    public void setWaveDirection(Integer waveDirection) {
        this.waveDirection = waveDirection;
    }

    public void setWavePeriod(Double wavePeriod) {
        this.wavePeriod = wavePeriod;
    }

    public void setSwellWaveHeight(Double swellWaveHeight) {
        this.swellWaveHeight = swellWaveHeight;
    }

    public void setSwellWaveDirection(Integer swellWaveDirection) {
        this.swellWaveDirection = swellWaveDirection;
    }

    public void setSwellWavePeriod(Double swellWavePeriod) {
        this.swellWavePeriod = swellWavePeriod;
    }

    public void setWindWaveHeight(Double windWaveHeight) {
        this.windWaveHeight = windWaveHeight;
    }

    public void setWindWaveDirection(Integer windWaveDirection) {
        this.windWaveDirection = windWaveDirection;
    }

    public void setWindWavePeriod(Double windWavePeriod) {
        this.windWavePeriod = windWavePeriod;
    }

    public void setSeaSurfaceTemperature(Double seaSurfaceTemperature) {
        this.seaSurfaceTemperature = seaSurfaceTemperature;
    }

    public void setTemperature2m(Double temperature2m) {
        this.temperature2m = temperature2m;
    }

    public void setApparentTemperature(Double apparentTemperature) {
        this.apparentTemperature = apparentTemperature;
    }

    public void setPrecipitation(Double precipitation) {
        this.precipitation = precipitation;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public void setPressureMsl(Double pressureMsl) {
        this.pressureMsl = pressureMsl;
    }

    public void setVisibility(Double visibility) {
        this.visibility = visibility;
    }

    public void setWindSpeed10m(Double windSpeed10m) {
        this.windSpeed10m = windSpeed10m;
    }

    public void setWindDirection10m(Integer windDirection10m) {
        this.windDirection10m = windDirection10m;
    }

    public void setWindGusts10m(Double windGusts10m) {
        this.windGusts10m = windGusts10m;
    }
}
