package com.example.AI_Projekt.error;

import com.example.AI_Projekt.rebrickable.RebrickableException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errors from the JSON API (classes marked @RestController) as RFC 9457 ProblemDetail.
 *
 * It is limited to @RestController on purpose. Pages (@Controller) are left to Spring Boot,
 * which shows templates/error.html; without the limit a page error came back as JSON and an
 * unknown address became a 500.
 *
 * The parent class already answers Spring's own errors (unknown address, wrong method,
 * unreadable body, missing parameter) with a ProblemDetail and the right status.
 */
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A @Valid request body failed: 400 with one message per field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are not valid.");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** A validated request parameter or path variable failed. */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more values are not valid.");
        return problem;
    }

    /** Rebrickable did not answer or refused. Our side is fine, so 502 and not 500. */
    @ExceptionHandler(RebrickableException.class)
    ProblemDetail handleRebrickable(RebrickableException ex) {
        log.warn("Rebrickable call failed (status {}): {}", ex.getStatus(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("Rebrickable is not available");
        problem.setDetail("Rebrickable could not be reached right now. Try again later.");
        return problem;
    }

    /** Thrown by method-level security (@PreAuthorize). URL rules are answered by Spring Security itself. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access denied");
        problem.setDetail("You are not allowed to do that.");
        return problem;
    }

    /** Everything else: the stack trace goes to the log, never to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error in the API", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Unexpected error");
        problem.setDetail("Something went wrong. Check the server logs for details.");
        return problem;
    }
}
