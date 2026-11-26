package com.example.AI_Projekt.service.sync;

import com.example.AI_Projekt.entity.SyncRun;
import com.example.AI_Projekt.entity.SyncStatus;
import com.example.AI_Projekt.repository.SyncRunRepo;
import com.example.AI_Projekt.service.sync.CatalogSyncService;
import com.example.AI_Projekt.service.sync.SyncAlreadyRunningException;
import com.example.AI_Projekt.service.sync.SyncRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** PB-07: only one sync at a time, whether it comes from the daily job or the admin button. */
class SyncRunLockTest {

    private CatalogSyncService catalogSyncService;
    private SyncRunService syncRunService;
    // Holds background work until the test runs it, so a run can be "in progress"
    private final List<Runnable> queued = new ArrayList<>();

    @BeforeEach
    void setUp() {
        catalogSyncService = mock(CatalogSyncService.class);
        SyncRunRepo repo = mock(SyncRunRepo.class);
        when(repo.save(any(SyncRun.class))).thenAnswer(call -> call.getArgument(0));
        syncRunService = new SyncRunService(catalogSyncService, repo, new ObjectMapper());
    }

    @Test
    void secondStartIsRefusedWhileTheFirstRuns() {
        assertThat(syncRunService.startInBackground(queued::add)).isTrue();
        assertThat(syncRunService.isRunning()).isTrue();

        assertThat(syncRunService.startInBackground(queued::add)).isFalse();
        assertThatThrownBy(() -> syncRunService.run()).isInstanceOf(SyncAlreadyRunningException.class);
        assertThat(queued).hasSize(1);

        queued.get(0).run();
        assertThat(syncRunService.isRunning()).isFalse();
        assertThat(syncRunService.startInBackground(queued::add)).isTrue();
    }

    @Test
    void lockIsReleasedWhenTheSyncFails() {
        when(catalogSyncService.syncAll()).thenThrow(new IllegalStateException("boom"));

        SyncRun run = syncRunService.run();

        assertThat(run.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(syncRunService.isRunning()).isFalse();
    }

    @Test
    void lockIsReleasedWhenTheExecutorRefuses() {
        assertThatThrownBy(() -> syncRunService.startInBackground(task -> {
            throw new RejectedExecutionException("full");
        })).isInstanceOf(RejectedExecutionException.class);

        assertThat(syncRunService.isRunning()).isFalse();
    }
}
