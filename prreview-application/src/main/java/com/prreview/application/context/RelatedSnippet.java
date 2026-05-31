package com.prreview.application.context;

import com.prreview.domain.model.pr.DiffHunk;

import java.util.List;

/**
 * A snippet of related code (L2 context layer) — e.g., a caller or callee of a changed symbol.
 */
public record RelatedSnippet(
        String filePath,
        int startLine,
        int endLine,
        String content,
        String relationshipType) {

    public RelatedSnippet {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("RelatedSnippet filePath must not be blank");
        }
    }
}
