package com.prreview.web.dto;

import com.prreview.domain.model.review.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for polling review task status.
 */
public record ReviewStatusResponse(
        UUID reviewId,
        ReviewStatus status,
        double progress,
        int filesTotal,
        int filesAnalyzed,
        OffsetDateTime startedAt,
        Integer estimatedRemainingSeconds,
        String resultUrl) {}
