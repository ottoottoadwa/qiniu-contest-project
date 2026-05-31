package com.prreview.domain.model.feedback;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User feedback on a specific risk item.
 * Feeds the confidence calibration loop.
 */
public record ReviewFeedback(
        String id,
        String riskItemId,
        String reviewId,
        String patternKey,
        FeedbackVerdict verdict,
        String comment,
        String submittedBy,
        OffsetDateTime createdAt) {

    public ReviewFeedback {
        if (riskItemId == null || riskItemId.isBlank()) {
            throw new IllegalArgumentException("ReviewFeedback riskItemId must not be blank");
        }
        if (verdict == null) {
            throw new IllegalArgumentException("ReviewFeedback verdict must not be null");
        }
    }

    /** Factory for creating a new feedback entry. */
    public static ReviewFeedback create(String riskItemId, String reviewId,
                                        String patternKey, FeedbackVerdict verdict,
                                        String comment, String submittedBy) {
        return new ReviewFeedback(
                UUID.randomUUID().toString(), riskItemId, reviewId,
                patternKey, verdict, comment, submittedBy, OffsetDateTime.now());
    }
}
