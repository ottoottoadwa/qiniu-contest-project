package com.prreview.domain.model.risk;

import java.util.List;

/**
 * A concrete suggestion for fixing a risk item.
 * Produced by the AI suggestion generation task.
 */
public record ReviewSuggestion(
        String riskItemId,
        String explanation,
        String recommendation,
        String suggestedPatch,
        List<String> references) {

    public ReviewSuggestion {
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("ReviewSuggestion explanation must not be blank");
        }
        if (recommendation == null || recommendation.isBlank()) {
            throw new IllegalArgumentException("ReviewSuggestion recommendation must not be blank");
        }
        references = references == null ? List.of() : List.copyOf(references);
    }
}
