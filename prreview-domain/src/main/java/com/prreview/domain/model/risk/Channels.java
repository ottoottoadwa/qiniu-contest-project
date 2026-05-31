package com.prreview.domain.model.risk;

/**
 * Which detection channels contributed to a risk signal.
 * Used by ConfidenceScoringService to determine base confidence.
 */
public enum Channels {
    /** Only the rule engine detected this risk. */
    RULE_ONLY,
    /** Only the AI model detected this risk. */
    AI_ONLY,
    /** Both rule engine and AI model detected this risk (highest confidence). */
    BOTH
}
