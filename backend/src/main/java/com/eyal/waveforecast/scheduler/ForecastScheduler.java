package com.eyal.waveforecast.scheduler;

import com.eyal.waveforecast.service.ForecastIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ForecastScheduler {

    private static final Logger log = LoggerFactory.getLogger(ForecastScheduler.class);

    private final ForecastIngestionService ingestionService;

    public ForecastScheduler(ForecastIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Scheduled(cron = "${forecast.ingestion.cron}", zone = "Asia/Jerusalem")
    public void hourlyIngestion() {
        log.info("Starting scheduled ingestion");
        ingestionService.ingestAll();
    }

    /**
     * Warm start. Without this, a fresh boot serves an empty database until
     * the top of the next hour — which during development is exactly when
     * you're trying to look at the UI.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        log.info("Startup ingestion");
        ingestionService.ingestAll();
    }
}
