package com.eyal.waveforecast.repository;

import com.eyal.waveforecast.domain.ForecastSnapshot;

import java.util.List;

public interface ForecastSnapshotBatch {
    void upsertAll(List<ForecastSnapshot> rows);
}
