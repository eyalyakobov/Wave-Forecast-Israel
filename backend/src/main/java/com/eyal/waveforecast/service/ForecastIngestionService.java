package com.eyal.waveforecast.service;

import com.eyal.waveforecast.client.OpenMeteoClient;
import com.eyal.waveforecast.client.dto.OpenMeteoDtos.MarineResponse;
import com.eyal.waveforecast.client.dto.OpenMeteoDtos.WeatherResponse;
import com.eyal.waveforecast.domain.ForecastSnapshot;
import com.eyal.waveforecast.domain.Spot;
import com.eyal.waveforecast.repository.ForecastSnapshotRepository;
import com.eyal.waveforecast.repository.SpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pulls fresh data for every active spot and persists it.
 *
 * <p>Design note: this runs on a schedule rather than lazily on request. The
 * cost is 12 spots x 2 endpoints per hour — trivial. The benefit is that user
 * requests never wait on a third-party API, and an Open-Meteo outage degrades
 * to slightly stale data instead of an error page.
 */
@Service
public class ForecastIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ForecastIngestionService.class);

    /** Open-Meteo emits local time without an offset: "2026-08-20T14:00". */
    private static final DateTimeFormatter OM_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final OpenMeteoClient client;
    private final SpotRepository spotRepository;
    private final ForecastSnapshotRepository snapshotRepository;
    private final ZoneId zone = ZoneId.of("Asia/Jerusalem");

    public ForecastIngestionService(OpenMeteoClient client,
                                    SpotRepository spotRepository,
                                    ForecastSnapshotRepository snapshotRepository) {
        this.client = client;
        this.spotRepository = spotRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public void ingestAll() {
        List<Spot> spots = spotRepository.findByActiveTrueOrderByDisplayOrderAsc();
        int ok = 0;

        for (Spot spot : spots) {
            try {
                ingestOne(spot);
                ok++;
            } catch (Exception e) {
                // One bad spot must not abort the run. Log and continue —
                // eleven fresh spots beat zero.
                log.error("Ingestion failed for spot {}: {}", spot.getSlug(), e.getMessage());
            }
        }
        log.info("Ingestion complete: {}/{} spots", ok, spots.size());
    }

    @Transactional
    public void ingestOne(Spot spot) {
        MarineResponse marine = client.fetchMarine(
                spot.getMarineLatitude(), spot.getMarineLongitude());
        WeatherResponse weather = client.fetchWeather(
                spot.getShoreLatitude(), spot.getShoreLongitude());

        List<ForecastSnapshot> rows = merge(spot, marine, weather);
        snapshotRepository.upsertAll(rows);

        log.debug("Ingested {} hours for {}", rows.size(), spot.getSlug());
    }

    /**
     * Zips the parallel arrays from both endpoints into one row per hour.
     *
     * <p>The two responses cover the same window but are indexed
     * independently, so we key the weather side by timestamp rather than
     * assuming index alignment. Assuming alignment works right up until one
     * endpoint returns a different forecast_days and then silently corrupts
     * every reading.
     */
    private List<ForecastSnapshot> merge(Spot spot,
                                         MarineResponse marine,
                                         WeatherResponse weather) {
        Map<LocalDateTime, Integer> weatherIndex = new HashMap<>();
        List<String> weatherTimes = weather.hourly().time();
        for (int i = 0; i < weatherTimes.size(); i++) {
            weatherIndex.put(LocalDateTime.parse(weatherTimes.get(i), OM_TIME), i);
        }

        List<String> marineTimes = marine.hourly().time();
        List<ForecastSnapshot> rows = new ArrayList<>(marineTimes.size());

        for (int i = 0; i < marineTimes.size(); i++) {
            LocalDateTime local = LocalDateTime.parse(marineTimes.get(i), OM_TIME);

            ForecastSnapshot row = new ForecastSnapshot();
            row.setSpot(spot);
            row.setForecastTime(local.atZone(zone).toInstant());
            row.setModelSource(resolveModel(local));

            var mh = marine.hourly();
            row.setWaveHeight(at(mh.waveHeight(), i));
            row.setWaveDirection(at(mh.waveDirection(), i));
            row.setWavePeriod(at(mh.wavePeriod(), i));
            row.setSwellWaveHeight(at(mh.swellWaveHeight(), i));
            row.setSwellWaveDirection(at(mh.swellWaveDirection(), i));
            row.setSwellWavePeriod(at(mh.swellWavePeriod(), i));
            row.setWindWaveHeight(at(mh.windWaveHeight(), i));
            row.setWindWaveDirection(at(mh.windWaveDirection(), i));
            row.setWindWavePeriod(at(mh.windWavePeriod(), i));
            row.setSeaSurfaceTemperature(at(mh.seaSurfaceTemperature(), i));

            Integer wi = weatherIndex.get(local);
            if (wi != null) {
                var wh = weather.hourly();
                row.setTemperature2m(at(wh.temperature2m(), wi));
                row.setApparentTemperature(at(wh.apparentTemperature(), wi));
                row.setPrecipitation(at(wh.precipitation(), wi));
                row.setWeatherCode(at(wh.weatherCode(), wi));
                row.setPressureMsl(at(wh.pressureMsl(), wi));
                row.setVisibility(at(wh.visibility(), wi));
                row.setWindSpeed10m(at(wh.windSpeed10m(), wi));
                row.setWindDirection10m(at(wh.windDirection10m(), wi));
                row.setWindGusts10m(at(wh.windGusts10m(), wi));
            }

            rows.add(row);
        }
        return rows;
    }

    /**
     * Records which model most likely produced this value.
     *
     * <p>Open-Meteo doesn't tell us directly. What we know is that the 5 km
     * European model covers the Mediterranean for roughly three days, after
     * which the API falls back to the 28 km global model. That boundary is
     * the honest place to start lowering the confidence shown in the UI.
     *
     * <p>Treat this as an approximation, not ground truth.
     */
    private String resolveModel(LocalDateTime forecastTime) {
        long days = java.time.Duration.between(
                LocalDateTime.now(zone), forecastTime).toDays();
        return days < 3 ? "meteofrance_currents_5km" : "global_28km";
    }

    /** Null-safe indexed read — any element in any array may be absent. */
    private static <T> T at(List<T> list, int i) {
        return (list == null || i >= list.size()) ? null : list.get(i);
    }
}
