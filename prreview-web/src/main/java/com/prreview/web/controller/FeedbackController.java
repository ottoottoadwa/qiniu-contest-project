package com.prreview.web.controller;

import com.prreview.application.port.in.SubmitFeedbackUseCase;
import com.prreview.web.dto.FeedbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for submitting feedback on risk items.
 * Drives the confidence calibration loop.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Risk item feedback for calibration")
public class FeedbackController {

    private final SubmitFeedbackUseCase submitFeedback;

    /**
     * Submits user feedback on a specific risk item.
     * Returns 201 Created with a Location header pointing to the feedback resource.
     */
    @PostMapping(value = "/v1/{reviewId}/risk-items/{riskItemId}/feedbacks",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit feedback on a risk item",
               description = "Records user verdict (ACCEPTED/FALSE_POSITIVE/IGNORED) to calibrate future confidence scores.")
    @ApiResponse(responseCode = "201", description = "Feedback recorded")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Review or risk item not found")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable @NotNull UUID reviewId,
            @PathVariable @NotBlank String riskItemId,
            @Valid @RequestBody FeedbackRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        SubmitFeedbackUseCase.SubmitFeedbackCommand command =
                new SubmitFeedbackUseCase.SubmitFeedbackCommand(
                        reviewId, riskItemId, request.verdict(), request.comment(), userId);

        UUID feedbackId = submitFeedback.submitFeedback(command);

        URI location = URI.create("/api/reviews/v1/" + reviewId
                + "/risk-items/" + riskItemId + "/feedbacks/" + feedbackId);

        return ResponseEntity.created(location).build();
    }
}
