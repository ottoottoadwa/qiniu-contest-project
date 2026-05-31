package com.prreview.domain.model.pr;

/**
 * A single contiguous block of diff content (a "hunk") within a file change.
 * Immutable value object.
 */
public record DiffHunk(
        int oldStart,
        int oldLines,
        int newStart,
        int newLines,
        String content) {

    public DiffHunk {
        if (content == null) {
            throw new IllegalArgumentException("DiffHunk content must not be null");
        }
    }
}
