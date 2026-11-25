package com.example.AI_Projekt.service.sync;

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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the catalog sync and records the outcome in {@code sync_run}.
 * <p>
 * Deliberately NOT {@code @Transactional}: the sync runs in its own transaction inside
 * {@link CatalogSyncService}. When that transaction rolls back, the {@code sync_run} row
 * written here must survive, otherwise a failed run would leave no trace.
 * <p>
 * Only one run at a time (PB-07): the daily job and the admin button share one lock. The lock
 * lives in memory, which is enough while the app runs as a single instance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRunService {

    static final int MAX_ERROR_LENGTH = 2000;

    private final CatalogSyncService catalogSyncService;
    private final SyncRunRepo syncRunRepo;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Runs the sync now, in this thread. Throws when another run is already going. */
    public SyncRun run() {
        if (!running.compareAndSet(false, true)) {
            throw new SyncAlreadyRunningException();
        }
        try {
            return doRun();
        } finally {
            running.set(false);
        }
    }

    /**
     * Takes the lock now and runs the sync on {@code executor}, so a web request does not wait
     * minutes for it. Returns false, and starts nothing, when a run is already going.
     */
    public boolean startInBackground(Executor executor) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        try {
            executor.execute(() -> {
                try {
                    doRun();
                } finally {
                    running.set(false);
                }
            });
            return true;
        } catch (RejectedExecutionException ex) {
            running.set(false);
            throw ex;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public List<SyncRun> recentRuns() {
        return syncRunRepo.findTop10ByOrderByIdDesc();
    }

    private SyncRun doRun() {
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
