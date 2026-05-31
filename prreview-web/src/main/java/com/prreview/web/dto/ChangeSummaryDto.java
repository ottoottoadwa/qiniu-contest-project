package com.prreview.web.dto;

import com.prreview.domain.model.review.ChangeType;

import java.util.List;

/**
 * Response DTO for the PR change summary.
 */
public record ChangeSummaryDto(
        String headline,
        String inferredPurpose,
        List<String> affectedModules,
        ChangeType primaryType,
        List<String> riskHighlights) {}
