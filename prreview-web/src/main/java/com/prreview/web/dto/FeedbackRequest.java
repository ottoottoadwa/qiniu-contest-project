package com.prreview.web.dto;

import com.prreview.domain.model.feedback.FeedbackVerdict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting feedback on a risk item.
 */
public record FeedbackRequest(
        @NotNull(message = "verdict must not be null")
        FeedbackVerdict verdict,

        @Size(max = 1000, message = "comment must not exceed 1000 characters")
        String comment) {}
