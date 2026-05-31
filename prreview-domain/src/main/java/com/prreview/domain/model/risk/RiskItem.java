package com.prreview.domain.model.risk;

import java.util.UUID;

/**
 * A finalized risk item after cross-validation and confidence scoring.
 * Immutable value object stored as part of the Review aggregate.
 */
public record RiskItem(
        String id,
        String reviewId,
        String filePath,
        int startLine,
        int endLine,
        RiskCategory category,
        Severity severity,
        Confidence confidence,
        double confidenceScore,
        DetectionSource detectionSource,
        String ruleId,
        String patternKey,
        String description,
        String rationale,
        ReviewSuggestion suggestion) {

    public RiskItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("RiskItem id must not be blank");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("RiskItem filePath must not be blank");
        }
    }

    /** Factory for creating a new risk item with a generated ID. */
    public static RiskItem create(String reviewId, String filePath,
                                  int startLine, int endLine,
                                  RiskCategory category, Severity severity,
                                  Confidence confidence, double confidenceScore,
                                  DetectionSource detectionSource,
                                  String ruleId, String patternKey,
                                  String description, String rationale) {
        return new RiskItem(
                UUID.randomUUID().toString(), reviewId, filePath,
                startLine, endLine, category, severity,
                confidence, confidenceScore, detectionSource,
                ruleId, patternKey, description, rationale, null);
    }

    /** Returns a copy with the suggestion attached. */
    public RiskItem withSuggestion(ReviewSuggestion suggestion) {
        return new RiskItem(id, reviewId, filePath, startLine, endLine,
                category, severity, confidence, confidenceScore,
                detectionSource, ruleId, patternKey, description, rationale, suggestion);
    }
}
