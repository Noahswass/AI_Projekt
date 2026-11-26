package com.example.AI_Projekt.service.sync;
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
        SyncRun run;
        try {
            run = syncRunService.run();
        } catch (SyncAlreadyRunningException ex) {
            // An admin started one by hand shortly before; that run covers today.
            log.warn("Skipping the daily catalog sync: a run is already going");
            return;
        }
        log.info("Catalog sync job completed, sync_run {} has status {}", run.getId(), run.getStatus());
    }

}
