package com.prreview.domain.model.risk;

/** Which detection channel(s) identified a risk item. */
public enum DetectionSource {
    /** Identified by the static rule engine only. */
    RULE,
    /** Identified by the AI model only. */
    AI,
    /** Identified by both rule engine and AI model (highest confidence). */
    BOTH
}
