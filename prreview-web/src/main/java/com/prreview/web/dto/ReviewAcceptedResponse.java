package com.prreview.web.dto;

import com.prreview.domain.model.review.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO returned immediately after accepting a review task (202 Accepted).
 */
public record ReviewAcceptedResponse(
        UUID reviewId,
        ReviewStatus status,
        String statusUrl,
        OffsetDateTime submittedAt) {}
