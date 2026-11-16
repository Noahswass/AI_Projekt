package com.example.AI_Projekt.service;
import com.example.AI_Projekt.entity.SyncRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogSyncJob {
    private final SyncRunService syncRunService;

    public CatalogSyncJob(SyncRunService syncRunService) {
        this.syncRunService = syncRunService;
    }
    @Scheduled(cron = "${app.sync.cron}", zone = "${app.sync.zone}")
    public void runDailySync() {
        log.info("Starting catalog sync job");
        SyncRun run = syncRunService.run();
        log.info("Catalog sync job completed, sync_run {} has status {}", run.getId(), run.getStatus());
    }

}
