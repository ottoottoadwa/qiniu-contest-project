package com.prreview.domain.model.review;

/** Analysis depth profile controlling model routing and context assembly. */
public enum AnalysisProfile {
    /** All fast-tier model, skip L2 context. Quick preview. */
    FAST,
    /** Fast-tier default + slow-tier cascade for critical items. Daily use. */
    STANDARD,
    /** Full slow-tier for critical files + complete L2 context. Security-sensitive releases. */
    DEEP
}
