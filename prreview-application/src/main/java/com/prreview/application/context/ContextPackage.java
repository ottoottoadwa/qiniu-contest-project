package com.prreview.application.context;

import com.prreview.domain.model.pr.DiffHunk;

import java.util.List;

/**
 * Assembled context package for a single file analysis.
 * Contains all context layers (L0–L4) within the token budget.
 */
public record ContextPackage(
        String filePath,
        List<DiffHunk> hunks,               // L0: diff hunks
        String enclosingDefinitions,         // L1: full function/class definitions
        List<RelatedSnippet> related,        // L2: callers/callees (on-demand)
        PrSemantics prSemantics,             // L3: PR title/description/intent
        List<KnowledgeChunk> knowledge,      // L4: team knowledge base (optional)
        int estimatedTokens) {

    public ContextPackage {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("ContextPackage filePath must not be blank");
        }
        hunks = hunks == null ? List.of() : List.copyOf(hunks);
        related = related == null ? List.of() : List.copyOf(related);
        knowledge = knowledge == null ? List.of() : List.copyOf(knowledge);
        enclosingDefinitions = enclosingDefinitions == null ? "" : enclosingDefinitions;
        prSemantics = prSemantics == null ? PrSemantics.empty() : prSemantics;
    }

    /**
     * Renders the context package as a single string for LLM consumption.
     * Includes all available layers in priority order.
     */
    public String render() {
        StringBuilder sb = new StringBuilder();

        // L3: PR intent first — helps model understand "why"
        if (prSemantics != null && !prSemantics.title().isBlank()) {
            sb.append("## PR Context\n");
            sb.append("Title: ").append(prSemantics.title()).append("\n");
            if (!prSemantics.description().isBlank()) {
                sb.append("Description: ").append(prSemantics.description()).append("\n");
            }
            sb.append("\n");
        }

        // L0: diff hunks
        sb.append("## File: ").append(filePath).append("\n");
        sb.append("### Changes (diff)\n");
        for (DiffHunk hunk : hunks) {
            sb.append("@@ -").append(hunk.oldStart()).append(",").append(hunk.oldLines())
              .append(" +").append(hunk.newStart()).append(",").append(hunk.newLines()).append(" @@\n");
            sb.append(hunk.content()).append("\n");
        }

        // L1: enclosing definitions
        if (!enclosingDefinitions.isBlank()) {
            sb.append("\n### Enclosing Definitions (L1)\n");
            sb.append(enclosingDefinitions).append("\n");
        }

        // L2: related snippets
        if (!related.isEmpty()) {
            sb.append("\n### Related Code (L2)\n");
            for (RelatedSnippet snippet : related) {
                sb.append("// ").append(snippet.filePath())
                  .append(":").append(snippet.startLine())
                  .append(" [").append(snippet.relationshipType()).append("]\n");
                sb.append(snippet.content()).append("\n");
            }
        }

        // L4: knowledge chunks
        if (!knowledge.isEmpty()) {
            sb.append("\n### Team Knowledge (L4)\n");
            for (KnowledgeChunk chunk : knowledge) {
                sb.append(chunk.content()).append("\n");
            }
        }

        return sb.toString();
    }
}
