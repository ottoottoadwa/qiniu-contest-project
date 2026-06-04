package com.prreview.infrastructure.github;

import com.prreview.domain.model.pr.RepositoryRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Service for posting comments to GitHub PRs.
 * Uses GitHub REST API v3 to create issue comments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubCommentService {

    private final RestClient gitHubRestClient;

    /**
     * Posts a comment to a PR.
     *
     * @param repo   repository reference
     * @param prNumber PR number
     * @param body   comment body (Markdown supported)
     */
    public void postComment(RepositoryRef repo, int prNumber, String body) {
        log.info("Posting comment to PR: {}/#{}", repo.toSlashNotation(), prNumber);

        try {
            Map<String, String> request = Map.of("body", body);

            gitHubRestClient.post()
                    .uri("/repos/{owner}/{repo}/issues/{number}/comments",
                            repo.owner(), repo.name(), prNumber)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Comment posted successfully to PR {}/#{}", repo.toSlashNotation(), prNumber);
        } catch (Exception e) {
            log.error("Failed to post comment to PR {}/#{}: {}",
                    repo.toSlashNotation(), prNumber, e.getMessage(), e);
            throw new CommentPostException("Failed to post comment", e);
        }
    }

    /**
     * Posts a reaction to a comment.
     *
     * @param repo      repository reference
     * @param commentId comment ID
     * @param reaction  reaction type (e.g., "+1", "eyes", "rocket")
     */
    public void postReaction(RepositoryRef repo, long commentId, String reaction) {
        log.debug("Posting reaction '{}' to comment {}", reaction, commentId);

        try {
            Map<String, String> request = Map.of("content", reaction);

            gitHubRestClient.post()
                    .uri("/repos/{owner}/{repo}/issues/comments/{commentId}/reactions",
                            repo.owner(), repo.name(), commentId)
                    .header("Accept", "application/vnd.github.squirrel-girl-preview+json")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Reaction posted successfully");
        } catch (Exception e) {
            log.warn("Failed to post reaction: {}", e.getMessage());
            // Don't throw - reactions are non-critical
        }
    }

    public static class CommentPostException extends RuntimeException {
        public CommentPostException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
