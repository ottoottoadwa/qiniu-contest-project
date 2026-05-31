package com.prreview.domain.model.risk;

/** Category of risk identified in a code change. */
public enum RiskCategory {
    CORRECTNESS,
    SECURITY,
    PERFORMANCE,
    MAINTAINABILITY;

    /** Security and correctness have higher weight in confidence scoring. */
    public boolean isSecurityOrCorrectness() {
        return this == SECURITY || this == CORRECTNESS;
    }
}
