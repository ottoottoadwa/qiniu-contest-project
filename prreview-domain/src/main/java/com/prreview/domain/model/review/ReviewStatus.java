package com.prreview.domain.model.review;

/** Lifecycle status of a PR review task. */
public enum ReviewStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    /** Analysis was truncated due to budget/size constraints. */
    PARTIAL
}
