package com.eyal.waveforecast.repository;

import com.eyal.waveforecast.domain.ForecastSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Batch upsert via plain JDBC.
 *
 * <p>Why not JPA here: each run writes ~168 rows per spot, and every one of
 * them collides with an existing row from the previous run. Doing that through
 * the entity manager means a SELECT per row to check existence, then a
 * managed-entity merge — roughly 2,000 round trips per hour for no benefit.
 *
 * <p>ON CONFLICT ... DO UPDATE pushes the whole decision into Postgres and
 * gives the table its real semantics: one row per (spot, hour), always
 * holding the most recent forecast for that hour. The unique constraint in
 * V1__init_schema.sql is what makes this work.
 */
@Repository
public class ForecastSnapshotBatchImpl implements ForecastSnapshotBatch {

    private static final String SQL = """
            INSERT INTO forecast_snapshot (
                spot_id, forecast_time, fetched_at, model_source,
                wave_height, wave_direction, wave_period,
                swell_wave_height, swell_wave_direction, swell_wave_period,
                wind_wave_height, wind_wave_direction, wind_wave_period,
                sea_surface_temperature,
                temperature_2m, apparent_temperature, precipitation,
                weather_code, pressure_msl, visibility,
                wind_speed_10m, wind_direction_10m, wind_gusts_10m
            ) VALUES (?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (spot_id, forecast_time) DO UPDATE SET
                fetched_at              = now(),
                model_source            = EXCLUDED.model_source,
                wave_height             = EXCLUDED.wave_height,
                wave_direction          = EXCLUDED.wave_direction,
                wave_period             = EXCLUDED.wave_period,
                swell_wave_height       = EXCLUDED.swell_wave_height,
                swell_wave_direction    = EXCLUDED.swell_wave_direction,
                swell_wave_period       = EXCLUDED.swell_wave_period,
                wind_wave_height        = EXCLUDED.wind_wave_height,
                wind_wave_direction     = EXCLUDED.wind_wave_direction,
                wind_wave_period        = EXCLUDED.wind_wave_period,
                sea_surface_temperature = EXCLUDED.sea_surface_temperature,
                temperature_2m          = EXCLUDED.temperature_2m,
                apparent_temperature    = EXCLUDED.apparent_temperature,
                precipitation           = EXCLUDED.precipitation,
                weather_code            = EXCLUDED.weather_code,
                pressure_msl            = EXCLUDED.pressure_msl,
                visibility              = EXCLUDED.visibility,
                wind_speed_10m          = EXCLUDED.wind_speed_10m,
                wind_direction_10m      = EXCLUDED.wind_direction_10m,
                wind_gusts_10m          = EXCLUDED.wind_gusts_10m
            """;

    private final JdbcTemplate jdbc;

    public ForecastSnapshotBatchImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsertAll(List<ForecastSnapshot> rows) {
        jdbc.batchUpdate(SQL, rows, 100, (ps, r) -> {
            int i = 1;
            ps.setLong(i++, r.getSpot().getId());
            ps.setTimestamp(i++, Timestamp.from(r.getForecastTime()));
            ps.setString(i++, r.getModelSource());
            setDouble(ps, i++, r.getWaveHeight());
            setInt(ps, i++, r.getWaveDirection());
            setDouble(ps, i++, r.getWavePeriod());
            setDouble(ps, i++, r.getSwellWaveHeight());
            setInt(ps, i++, r.getSwellWaveDirection());
            setDouble(ps, i++, r.getSwellWavePeriod());
            setDouble(ps, i++, r.getWindWaveHeight());
            setInt(ps, i++, r.getWindWaveDirection());
            setDouble(ps, i++, r.getWindWavePeriod());
            setDouble(ps, i++, r.getSeaSurfaceTemperature());
            setDouble(ps, i++, r.getTemperature2m());
            setDouble(ps, i++, r.getApparentTemperature());
            setDouble(ps, i++, r.getPrecipitation());
            setInt(ps, i++, r.getWeatherCode());
            setDouble(ps, i++, r.getPressureMsl());
            setDouble(ps, i++, r.getVisibility());
            setDouble(ps, i++, r.getWindSpeed10m());
            setInt(ps, i++, r.getWindDirection10m());
            setDouble(ps, i, r.getWindGusts10m());
        });
    }

    private static void setDouble(java.sql.PreparedStatement ps, int i, Double v)
            throws java.sql.SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.NUMERIC);
        else ps.setDouble(i, v);
    }

    private static void setInt(java.sql.PreparedStatement ps, int i, Integer v)
            throws java.sql.SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.INTEGER);
        else ps.setInt(i, v);
    }
}
