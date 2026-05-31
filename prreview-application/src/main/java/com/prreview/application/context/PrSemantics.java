package com.prreview.application.context;

/**
 * PR-level semantic context (L3 layer) — title, description, intent.
 */
public record PrSemantics(
        String title,
        String description,
        String author,
        String baseBranch,
        String headBranch) {

    public static PrSemantics empty() {
        return new PrSemantics("", "", "", "", "");
    }
}
