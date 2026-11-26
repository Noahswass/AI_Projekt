package com.example.AI_Projekt.service.sync;

/** A catalog sync was asked for while another one is still running. */
public class SyncAlreadyRunningException extends RuntimeException {
    public SyncAlreadyRunningException() {
        super("A catalog sync is already running");
    }
}
