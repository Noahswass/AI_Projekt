package com.example.AI_Projekt.rebrickable;

/**
 * A Rebrickable call failed: a non-retryable status, retries used up, or no response.
 * {@code status} is null when no HTTP response was received (timeout, connection error).
 */
public class RebrickableException extends RuntimeException {

    private final Integer status;

    public RebrickableException(String message, Integer status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
