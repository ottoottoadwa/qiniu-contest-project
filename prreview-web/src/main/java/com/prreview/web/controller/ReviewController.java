package com.prreview.web.controller;

import com.prreview.application.port.in.GetReviewQuery;
import com.prreview.application.port.in.SubmitReviewUseCase;
import com.prreview.application.service.GetReviewService;
import com.prreview.domain.model.review.Review;
import com.prreview.web.dto.ReviewAcceptedResponse;
import com.prreview.web.dto.ReviewResultResponse;
import com.prreview.web.dto.ReviewStatusResponse;
import com.prreview.web.dto.SubmitReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST controller for PR review operations.
 * Thin controller: validates input, delegates to use cases, maps responses.
 * No business logic here — all logic lives in the application/domain layers.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "PR review submission and result retrieval")
public class ReviewController {

    private final SubmitReviewUseCase submitReview;
    private final GetReviewQuery getReview;

    /**
     * Submits a PR review task asynchronously.
     * Returns 202 Accepted immediately with a polling URL.
     */
    @PostMapping(value = "/v1", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit a PR review task",
               description = "Accepts a PR review request and returns immediately with a polling URL. Analysis runs asynchronously.")
    @ApiResponse(responseCode = "202", description = "Review task accepted")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    public ResponseEntity<ReviewAcceptedResponse> submit(
            @Valid @RequestBody SubmitReviewRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        SubmitReviewUseCase.ReviewAccepted accepted =
                submitReview.submit(request.toCommand(), idempotencyKey);

        URI location = URI.create(accepted.statusUrl());
        ReviewAcceptedResponse body = new ReviewAcceptedResponse(
                accepted.reviewId(),
                com.prreview.domain.model.review.ReviewStatus.PENDING,
                accepted.statusUrl(),
                OffsetDateTime.now());

        return ResponseEntity.accepted().location(location).body(body);
    }

    /**
     * Polls the status of a review task.
     */
    @GetMapping(value = "/v1/{reviewId}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Poll review task status")
    @ApiResponse(responseCode = "200", description = "Status returned")
    @ApiResponse(responseCode = "404", description = "Review not found")
    public ResponseEntity<ReviewStatusResponse> status(
            @PathVariable @NotNull UUID reviewId) {

        GetReviewQuery.ReviewStatusView view = getReview.status(reviewId);
        ReviewStatusResponse response = new ReviewStatusResponse(
                view.reviewId(), view.status(), view.progress(),
                view.filesTotal(), view.filesAnalyzed(),
                view.startedAt(), view.estimatedRemainingSeconds(), view.resultUrl());

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(response);
    }

    /**
     * Returns the complete review result.
     * Returns 409 if the review is not yet completed.
     */
    @GetMapping(value = "/v1/{reviewId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get complete review result")
    @ApiResponse(responseCode = "200", description = "Review result returned")
    @ApiResponse(responseCode = "404", description = "Review not found")
    @ApiResponse(responseCode = "409", description = "Review not yet completed")
    public ResponseEntity<ReviewResultResponse> result(
            @PathVariable @NotNull UUID reviewId) {

        Review review = getReview.result(reviewId);
        return ResponseEntity.ok(ReviewResultResponse.from(review));
    }

    /**
     * Deletes a review record (soft delete).
     */
    @DeleteMapping(value = "/v1/{reviewId}")
    @Operation(summary = "Delete a review record")
    @ApiResponse(responseCode = "204", description = "Review deleted")
    @ApiResponse(responseCode = "404", description = "Review not found")
    public ResponseEntity<Void> delete(@PathVariable @NotNull UUID reviewId) {
        // For MVP: not implemented — returns 204 as placeholder
        return ResponseEntity.noContent().build();
    }
}
