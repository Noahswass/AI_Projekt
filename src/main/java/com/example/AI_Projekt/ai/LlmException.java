package com.example.AI_Projekt.ai;

import org.springframework.web.bind.annotation.ResponseBody;

public class LlmException extends RuntimeException {

    private final Integer status;

    private final String responseBody;

    public LlmException(String message, Integer status, String responseBody, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    public Integer getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}