package com.prreview.web.advice;

import com.prreview.application.service.GetReviewService;
import com.prreview.infrastructure.github.GitHubPrSourceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler using RFC 7807 ProblemDetail.
 * All exceptions are caught here — controllers never catch exceptions directly.
 *
 * <p>Security: error responses never expose stack traces, SQL errors, class names, or library versions.
 * Full exception details are logged server-side with a correlation ID.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://prreview.dev/problems/";

    /** Handles Bean Validation failures (400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Validation Failed");
        problem.setDetail("Request body validation failed");

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", safeMessage(fe)))
                .toList();
        problem.setProperty("errors", errors);

        return problem;
    }

    /** Handles review not found (404). */
    @ExceptionHandler(GetReviewService.ReviewNotFoundException.class)
    public ProblemDetail handleReviewNotFound(GetReviewService.ReviewNotFoundException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("Review not found: correlationId={}, message={}", correlationId, ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create(PROBLEM_BASE_URI + "resource-not-found"));
        problem.setTitle("Resource Not Found");
        problem.setDetail("The requested review was not found");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    /** Handles review not ready (409). */
    @ExceptionHandler(GetReviewService.ReviewNotReadyException.class)
    public ProblemDetail handleReviewNotReady(GetReviewService.ReviewNotReadyException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create(PROBLEM_BASE_URI + "review-not-ready"));
        problem.setTitle("Review Not Ready");
        problem.setDetail("The review is not yet completed. Poll the status endpoint first.");
        return problem;
    }

    /** Handles PR not found on GitHub (404). */
    @ExceptionHandler(GitHubPrSourceAdapter.PrNotFoundException.class)
    public ProblemDetail handlePrNotFound(GitHubPrSourceAdapter.PrNotFoundException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("PR not found: correlationId={}, message={}", correlationId, ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create(PROBLEM_BASE_URI + "resource-not-found"));
        problem.setTitle("Pull Request Not Found");
        problem.setDetail("The specified pull request was not found on GitHub");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    /** Handles illegal argument errors (400). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("Bad request: correlationId={}, message={}", correlationId, ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(PROBLEM_BASE_URI + "validation-error"));
        problem.setTitle("Bad Request");
        problem.setDetail("The request contains invalid parameters");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    /** Catch-all for unexpected errors (500). Never exposes internals. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unexpected error: correlationId={}", correlationId, ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create(PROBLEM_BASE_URI + "internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    private String safeMessage(FieldError fe) {
        return fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value";
    }
}
