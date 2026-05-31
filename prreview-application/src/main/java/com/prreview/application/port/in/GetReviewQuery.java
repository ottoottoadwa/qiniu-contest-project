package com.prreview.application.port.in;

import com.prreview.domain.model.review.Review;

import java.util.UUID;

/**
 * Inbound port for querying review status and results.
 * Implemented by GetReviewService in the application layer.
 */
public interface GetReviewQuery {

    /** Returns the current status of a review task. */
    ReviewStatusView status(UUID reviewId);

    /** Returns the complete review result. Throws if not yet completed. */
    Review result(UUID reviewId);

    /** Lightweight status view for polling. */
    record ReviewStatusView(
            UUID reviewId,
            com.prreview.domain.model.review.ReviewStatus status,
            double progress,
            int filesTotal,
            int filesAnalyzed,
            java.time.OffsetDateTime startedAt,
            Integer estimatedRemainingSeconds,
            String resultUrl) {}
}
