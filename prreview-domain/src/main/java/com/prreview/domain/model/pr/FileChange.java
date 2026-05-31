package com.prreview.domain.model.pr;

import java.util.List;

/**
 * Represents a single file changed within a pull request.
 * Immutable value object.
 */
public record FileChange(
        String path,
        ChangeType changeType,
        int additions,
        int deletions,
        List<DiffHunk> hunks,
        boolean patchTruncated) {

    public FileChange {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("FileChange path must not be blank");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("FileChange changeType must not be null");
        }
        hunks = hunks == null ? List.of() : List.copyOf(hunks);
    }

    /** Total lines changed (additions + deletions). */
    public int totalChangedLines() {
        return additions + deletions;
    }
}
