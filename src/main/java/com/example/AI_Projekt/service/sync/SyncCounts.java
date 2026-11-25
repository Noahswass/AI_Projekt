package com.example.AI_Projekt.service.sync;

/** Rows received and upserted per table in one sync run. Stored as JSON in {@code sync_run.counts_json}. */
public record SyncCounts(int themes, int colours, int sets, int parts, int minifigs) {
}
