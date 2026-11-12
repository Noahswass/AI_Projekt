package com.example.AI_Projekt.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The type Global exception handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle unexpected problem detail.
     *
     * @param ex the ex
     * @return the problem detail
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        // Log the stack trace server-side; never expose it to the client.
        var problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Unexpected error");
        problem.setDetail("Something went wrong. Check the server logs for details.");
        return problem;
    }
}

