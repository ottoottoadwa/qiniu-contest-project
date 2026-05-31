package com.prreview.application.context;

import com.prreview.domain.model.pr.DiffHunk;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Estimates token counts and plans context trimming within a budget.
 * Uses a simple character-based approximation (4 chars ≈ 1 token).
 */
@Service
public class TokenBudgetPlanner {

    /** Default token budget per file analysis (leaves room for system prompt + output). */
    private static final int DEFAULT_BUDGET = 8000;

    /** Rough approximation: 4 characters per token. */
    private static final int CHARS_PER_TOKEN = 4;

    /**
     * Estimates the total token count for a context package.
     */
    public int estimate(List<DiffHunk> hunks, String enclosingDefs,
                        List<RelatedSnippet> related, List<KnowledgeChunk> knowledge) {
        int chars = 0;
        for (DiffHunk hunk : hunks) {
            chars += hunk.content().length();
        }
        chars += enclosingDefs.length();
        for (RelatedSnippet snippet : related) {
            chars += snippet.content().length();
        }
        for (KnowledgeChunk chunk : knowledge) {
            chars += chunk.content().length();
        }
        return chars / CHARS_PER_TOKEN;
    }

    /**
     * Returns true if the estimated token count is within the default budget.
     */
    public boolean withinBudget(int estimatedTokens) {
        return estimatedTokens <= DEFAULT_BUDGET;
    }

    /**
     * Returns the default token budget.
     */
    public int defaultBudget() {
        return DEFAULT_BUDGET;
    }
}
