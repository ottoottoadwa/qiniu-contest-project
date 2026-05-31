package com.prreview.domain.model.pr;

/**
 * Domain representation of a GitHub Pull Request.
 * Immutable value object carrying all metadata needed for analysis.
 */
public record PullRequest(
        RepositoryRef repo,
        int number,
        String title,
        String description,
        String author,
        String baseSha,
        String headSha,
        PrState state) {

    public PullRequest {
        if (repo == null) {
            throw new IllegalArgumentException("PullRequest repo must not be null");
        }
        if (number <= 0) {
            throw new IllegalArgumentException("PullRequest number must be positive");
        }
    }
}
