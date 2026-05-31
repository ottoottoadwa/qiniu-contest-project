package com.prreview.domain.model.review;

import java.util.List;

/**
 * Structured summary of what a pull request does.
 * Produced by the AI summary task.
 */
public record ChangeSummary(
        String headline,
        String inferredPurpose,
        List<String> affectedModules,
        ChangeType primaryType,
        List<String> riskHighlights) {

    public ChangeSummary {
        if (headline == null || headline.isBlank()) {
            throw new IllegalArgumentException("ChangeSummary headline must not be blank");
        }
        affectedModules = affectedModules == null ? List.of() : List.copyOf(affectedModules);
        riskHighlights = riskHighlights == null ? List.of() : List.copyOf(riskHighlights);
    }
}
