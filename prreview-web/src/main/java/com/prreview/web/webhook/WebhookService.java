package com.prreview.web.webhook;

import com.prreview.application.review.ResultFormatter;
import com.prreview.application.async.ReviewTaskRunner;
import com.prreview.domain.model.pr.RepositoryRef;
import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import com.prreview.infrastructure.github.GitHubCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Processes GitHub webhook events.
 * Handles automatic PR review on PR open/update and manual triggers via comments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ReviewTaskRunner reviewTaskRunner;
    private final ReviewRepositoryPort reviewRepository;
    private final GitHubCommentService commentService;
    private final ResultFormatter resultFormatter;
    private final EntityManager entityManager;

    /**
     * Handles pull_request events.
     * Automatically triggers review when PR is opened or synchronized (updated).
     *
     * @param payload webhook payload
     * @return true if review was triggered, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean handlePullRequest(Map<String, Object> payload) {
        String action = (String) payload.get("action");

        // Trigger review on PR open or update
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            log.debug("Ignoring PR action: {}", action);
            return false;
        }

        Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
        Map<String, Object> repository = (Map<String, Object>) payload.get("repository");

        if (pullRequest == null || repository == null) {
            log.warn("Missing required fields in webhook payload");
            return false;
        }

        // Extract PR information
        String fullName = (String) repository.get("full_name");
        int prNumber = ((Number) pullRequest.get("number")).intValue();

        log.info("Auto-triggering review for PR: {}#{} (action: {})", fullName, prNumber, action);

        try {
            RepositoryRef repo = RepositoryRef.parse(fullName);

            // Trigger review asynchronously
            triggerReviewAsync(repo, prNumber);

            return true;
        } catch (Exception e) {
            log.error("Failed to trigger review: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handles issue_comment events.
     * Triggers review when comment contains "/review" command.
     *
     * @param payload webhook payload
     * @return true if review was triggered, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean handleIssueComment(Map<String, Object> payload) {
        String action = (String) payload.get("action");

        // Only process "created" comments
        if (!"created".equals(action)) {
            log.debug("Ignoring comment action: {}", action);
            return false;
        }

        Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
        Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
        Map<String, Object> repository = (Map<String, Object>) payload.get("repository");

        if (comment == null || issue == null || repository == null) {
            log.warn("Missing required fields in webhook payload");
            return false;
        }

        // Check if this is a PR (issues and PRs share the same API)
        Map<String, Object> pullRequest = (Map<String, Object>) issue.get("pull_request");
        if (pullRequest == null) {
            log.debug("Comment is on an issue, not a PR");
            return false;
        }

        String commentBody = (String) comment.get("body");
        if (commentBody == null || !commentBody.trim().toLowerCase().contains("/review")) {
            log.debug("Comment does not contain /review command");
            return false;
        }

        // Extract PR information
        String fullName = (String) repository.get("full_name");
        int prNumber = ((Number) issue.get("number")).intValue();
        long commentId = ((Number) comment.get("id")).longValue();

        log.info("Triggering review for PR: {}#{} (comment: {})", fullName, prNumber, commentId);

        try {
            RepositoryRef repo = RepositoryRef.parse(fullName);

            // React with "eyes" to acknowledge
            commentService.postReaction(repo, commentId, "eyes");

            // Trigger review asynchronously
            triggerReviewAsync(repo, prNumber);

            return true;
        } catch (Exception e) {
            log.error("Failed to trigger review: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Triggers a PR review asynchronously and posts results as a comment.
     * Uses NOT_SUPPORTED propagation to avoid reading stale data from outer transaction.
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void triggerReviewAsync(RepositoryRef repo, int prNumber) {
        log.info("Starting async review: {}#{}", repo.toSlashNotation(), prNumber);

        try {
            // Create review
            Review review = Review.create(
                    repo.toSlashNotation(),
                    prNumber,
                    AnalysisProfile.STANDARD,
                    null // idempotency key
            );
            reviewRepository.save(review);

            // Post initial comment
            String initialComment = "🔍 **审查已启动！**\n\n" +
                    "正在分析您的 PR，这可能需要几分钟时间。\n\n" +
                    "审查 ID: `" + review.getId() + "`";
            commentService.postComment(repo, prNumber, initialComment);

            // Execute review
            reviewTaskRunner.runAsync(review.getId(), Set.of());

            // Wait for completion with shorter intervals
            int maxWaitSeconds = 120; // 2 minutes (reduced from 5)
            int waited = 0;
            Review updated = null;

            while (waited < maxWaitSeconds) {
                Thread.sleep(2000); // Check every 2 seconds (reduced from 5)
                waited += 2;

                // Clear the persistence context to force a fresh query
                entityManager.clear();

                updated = reviewRepository.findById(review.getId()).orElse(null);
                if (updated == null) {
                    log.error("Review disappeared: {}", review.getId());
                    return;
                }

                log.debug("Review status check: reviewId={}, status={}, waited={}s",
                         review.getId(), updated.getStatus(), waited);

                if (updated.getStatus() == com.prreview.domain.model.review.ReviewStatus.COMPLETED) {
                    // Post results
                    String resultComment = resultFormatter.formatAsComment(updated);
                    commentService.postComment(repo, prNumber, resultComment);
                    log.info("Review completed and posted: {}#{}", repo.toSlashNotation(), prNumber);
                    return;
                } else if (updated.getStatus() == com.prreview.domain.model.review.ReviewStatus.FAILED) {
                    // Post error
                    String errorComment = resultFormatter.formatError(updated.getFailureReason());
                    commentService.postComment(repo, prNumber, errorComment);
                    log.error("Review failed: {}#{}", repo.toSlashNotation(), prNumber);
                    return;
                }
            }

            // Timeout - but check one more time
            updated = reviewRepository.findById(review.getId()).orElse(null);
            if (updated != null && updated.getStatus() == com.prreview.domain.model.review.ReviewStatus.COMPLETED) {
                String resultComment = resultFormatter.formatAsComment(updated);
                commentService.postComment(repo, prNumber, resultComment);
                log.info("Review completed (after timeout check): {}#{}", repo.toSlashNotation(), prNumber);
            } else {
                log.warn("Review timed out: {}#{}", repo.toSlashNotation(), prNumber);
                String timeoutComment = "⏱️ **审查超时**\n\n" +
                        "审查时间超过预期。" +
                        "您可以通过 `/api/reviews/" + review.getId() + "` 查看状态";
                commentService.postComment(repo, prNumber, timeoutComment);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Review interrupted: {}#{}", repo.toSlashNotation(), prNumber);
        } catch (Exception e) {
            log.error("Review failed: {}#{}", repo.toSlashNotation(), prNumber, e);
            try {
                String errorComment = resultFormatter.formatError(e.getMessage());
                commentService.postComment(repo, prNumber, errorComment);
            } catch (Exception commentError) {
                log.error("Failed to post error comment: {}", commentError.getMessage());
            }
        }
    }
}
