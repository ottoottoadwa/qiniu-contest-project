package com.prreview.domain.model.risk;

/**
 * Three-level confidence classification for risk items.
 * Computed by ConfidenceScoringService from a raw score.
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Maps a raw confidence score (0.0–1.0) to a Confidence level.
     * Thresholds: HIGH >= 0.75, MEDIUM >= 0.45, LOW < 0.45.
     */
    public static Confidence fromScore(double score) {
        if (score >= 0.75) return HIGH;
        if (score >= 0.45) return MEDIUM;
        return LOW;
    }
}
