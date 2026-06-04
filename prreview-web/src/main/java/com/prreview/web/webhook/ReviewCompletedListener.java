package com.prreview.web.webhook;

import com.prreview.application.review.ResultFormatter;
import com.prreview.domain.model.pr.RepositoryRef;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import com.prreview.infrastructure.github.GitHubCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Listens for review completion events and posts results to GitHub.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCompletedListener {

    private final ReviewRepositoryPort reviewRepository;
    private final GitHubCommentService commentService;
    private final ResultFormatter resultFormatter;

    @Async("reviewTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCompleted(com.prreview.application.review.ReviewCompletedEvent event) {
        log.info("========== LISTENER INVOKED ==========");
        log.info("Event received in listener: reviewId={}, repo={}, pr={}, thread={}",
                event.getReviewId(), event.getRepository(), event.getPrNumber(),
                Thread.currentThread().getName());

        try {
            // Wait for transaction commit to fully propagate
            log.info("Waiting 5 seconds for transaction commit...");
            Thread.sleep(5000);

            // Use new transaction to read fresh data
            Review review = readReviewInNewTransaction(event.getReviewId());

            if (review == null) {
                log.error("Review not found: {}", event.getReviewId());
                return;
            }

            log.info("===== FORMATTING REVIEW RESULTS =====");
            log.info("Review ID: {}", review.getId());
            log.info("Review status: {}", review.getStatus());
            log.info("Review.getRiskItems().size() = {}", review.getRiskItems().size());

            // Post results
            RepositoryRef repo = RepositoryRef.parse(event.getRepository());
            String resultComment = resultFormatter.formatAsComment(review);

            log.info("===== FORMATTED COMMENT =====");
            log.info("Comment length: {}", resultComment.length());

            commentService.postComment(repo, event.getPrNumber(), resultComment);

            log.info("Review results posted to PR: {}#{}",
                    event.getRepository(), event.getPrNumber());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to post review results: reviewId={}", event.getReviewId(), e);

            // Try to post error comment
            try {
                RepositoryRef repo = RepositoryRef.parse(event.getRepository());
                String errorComment = resultFormatter.formatError(e.getMessage());
                commentService.postComment(repo, event.getPrNumber(), errorComment);
            } catch (Exception commentError) {
                log.error("Failed to post error comment: {}", commentError.getMessage());
            }
        }
    }

    /**
     * Read review in a new transaction to ensure fresh data.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected Review readReviewInNewTransaction(java.util.UUID reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review != null) {
            log.info("Read review in new transaction: status={}, riskItems={}",
                    review.getStatus(), review.getRiskItems().size());
            // Force load lazy collections
            review.getRiskItems().size();
        }
        return review;
    }
}
