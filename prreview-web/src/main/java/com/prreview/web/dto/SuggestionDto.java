package com.prreview.web.dto;

import java.util.List;

/**
 * Response DTO for a review suggestion.
 */
public record SuggestionDto(
        String explanation,
        String recommendation,
        String suggestedPatch,
        List<String> references) {}
