package com.prreview.web.webhook;

import com.prreview.application.review.ResultFormatter;
import com.prreview.application.async.ReviewTaskRunner;
import com.prreview.domain.model.pr.RepositoryRef;
import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.review.ReviewStatus;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import com.prreview.infrastructure.github.GitHubCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

            // Check if there's already a recent review (prevent duplicate triggers within 5 minutes)
            if (hasRecentReview(repo, prNumber)) {
                log.info("Skipping duplicate review - recent review exists for {}#{}", fullName, prNumber);
                return false;
            }

            // Trigger review asynchronously
            triggerReviewAsync(repo, prNumber);

            return true;
        } catch (Exception e) {
            log.error("Failed to trigger review: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if there's a recent review for this PR (within 5 minutes).
     */
    private boolean hasRecentReview(RepositoryRef repo, int prNumber) {
        Instant fiveMinutesAgo = Instant.now().minusSeconds(300);
        return reviewRepository.findAll().stream()
                .filter(r -> r.getRepository().equals(repo.toSlashNotation()))
                .filter(r -> r.getPrNumber() == prNumber)
                .filter(r -> r.getCreatedAt().isAfter(fiveMinutesAgo))
                .anyMatch(r -> r.getStatus() == ReviewStatus.RUNNING || r.getStatus() == ReviewStatus.COMPLETED);
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

        // Ignore comments from bots (including ourselves) to prevent infinite loops
        Map<String, Object> user = (Map<String, Object>) comment.get("user");
        if (user != null) {
            Object userType = user.get("type");
            if ("Bot".equals(userType)) {
                log.debug("Ignoring comment from bot user");
                return false;
            }
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
     * Triggers a PR review asynchronously.
     * Returns immediately - results will be posted via event listener.
     */
    @Async
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

            // Execute review (results will be posted by event listener)
            reviewTaskRunner.runAsync(review.getId(), Set.of());

            log.info("Review task submitted: reviewId={}, repo={}#{}",
                    review.getId(), repo.toSlashNotation(), prNumber);

        } catch (Exception e) {
            log.error("Review failed to start: {}#{}", repo.toSlashNotation(), prNumber, e);
            try {
                String errorComment = resultFormatter.formatError(e.getMessage());
                commentService.postComment(repo, prNumber, errorComment);
            } catch (Exception commentError) {
                log.error("Failed to post error comment: {}", commentError.getMessage());
            }
        }
    }
}
