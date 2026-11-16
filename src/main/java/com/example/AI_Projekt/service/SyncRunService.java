package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.SyncRun;
import com.example.AI_Projekt.entity.SyncStatus;
import com.example.AI_Projekt.repository.SyncRunRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Runs the catalog sync and records the outcome in {@code sync_run}.
 * <p>
 * Deliberately NOT {@code @Transactional}: the sync runs in its own transaction inside
 * {@link CatalogSyncService}. When that transaction rolls back, the {@code sync_run} row
 * written here must survive, otherwise a failed run would leave no trace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRunService {

    static final int MAX_ERROR_LENGTH = 2000;

    private final CatalogSyncService catalogSyncService;
    private final SyncRunRepo syncRunRepo;
    private final ObjectMapper objectMapper;

    public SyncRun run() {
        SyncRun run = new SyncRun();
        run.setStartedAt(Timestamp.from(Instant.now()));
        run.setStatus(SyncStatus.RUNNING);
        run = syncRunRepo.save(run);
        log.info("Catalog sync run {} started", run.getId());

        try {
            SyncCounts counts = catalogSyncService.syncAll();
            run.setCountsJson(toJson(counts));
            run.setStatus(SyncStatus.SUCCESS);
            log.info("Catalog sync run {} succeeded: {}", run.getId(), counts);
        } catch (Exception ex) {
            run.setStatus(SyncStatus.FAILED);
            run.setErrorMessage(describe(ex));
            log.error("Catalog sync run {} failed, all changes rolled back", run.getId(), ex);
        }

        run.setFinishedAt(Timestamp.from(Instant.now()));
        return syncRunRepo.save(run);
    }

    private String toJson(SyncCounts counts) {
        try {
            return objectMapper.writeValueAsString(counts);
        } catch (JsonProcessingException ex) {
            // The data is already committed; do not report the run as failed over the log format.
            return counts.toString();
        }
    }

    /** Exception type and message, plus the root cause when it says something different. */
    private String describe(Exception ex) {
        String text = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != ex && root.getMessage() != null && !text.contains(root.getMessage())) {
            text += " (cause: " + root.getClass().getSimpleName() + ": " + root.getMessage() + ")";
        }
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}
