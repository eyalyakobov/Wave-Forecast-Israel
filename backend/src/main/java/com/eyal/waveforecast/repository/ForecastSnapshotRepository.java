package com.eyal.waveforecast.repository;

import com.eyal.waveforecast.domain.ForecastSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ForecastSnapshotRepository
        extends JpaRepository<ForecastSnapshot, Long>, ForecastSnapshotBatch {

    List<ForecastSnapshot> findBySpotIdAndForecastTimeBetweenOrderByForecastTimeAsc(
            Long spotId, Instant from, Instant to);
}