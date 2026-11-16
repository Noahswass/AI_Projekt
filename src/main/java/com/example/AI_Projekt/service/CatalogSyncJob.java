package com.example.AI_Projekt.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogSyncJob {
    private final CatalogSyncService catalogSyncService;

    public CatalogSyncJob(CatalogSyncService catalogSyncService) {
        this.catalogSyncService = catalogSyncService;
    }
    @Scheduled(cron = "${app.sync.cron}", zone = "${app.sync.zone}")
    public void runDailySync() {
        log.info("Starting catalog sync job");
        boolean result = catalogSyncService.syncAll();
        log.info("Catalog sync job completed, all tables synced: {}", result);
    }

}
