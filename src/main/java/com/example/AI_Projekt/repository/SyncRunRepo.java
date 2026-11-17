package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.SyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncRunRepo extends JpaRepository<SyncRun, Integer> {

    /** Newest first. The id grows with every run, so it orders like started_at. */
    List<SyncRun> findTop10ByOrderByIdDesc();
}
