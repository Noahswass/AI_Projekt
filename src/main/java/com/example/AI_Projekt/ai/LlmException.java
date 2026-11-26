package com.example.AI_Projekt.ai;

public class LlmException extends RuntimeException {

    private final Integer status;

    public LlmException(String message, Integer status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}