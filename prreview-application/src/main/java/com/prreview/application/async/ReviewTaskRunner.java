package com.prreview.application.async;

import com.prreview.application.review.ReviewOrchestrator;
import com.prreview.domain.model.risk.RiskCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Runs review tasks asynchronously using Spring @Async.
 * With virtual threads enabled (spring.threads.virtual.enabled=true),
 * each task runs on a virtual thread — high concurrency at low cost.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewTaskRunner {

    private final ReviewOrchestrator orchestrator;

    /**
     * Submits a review task for asynchronous execution.
     * Returns immediately; the actual analysis runs in the background.
     *
     * @param reviewId   ID of the review to execute
     * @param categories risk categories to analyze
     */
    @Async("reviewTaskExecutor")
    public void runAsync(UUID reviewId, Set<RiskCategory> categories) {
        log.info("Async review task started: reviewId={}", reviewId);
        try {
            orchestrator.executeReview(reviewId, categories);
        } catch (Exception e) {
            // Orchestrator handles its own error state; this is a safety net
            log.error("Unhandled error in async review task: reviewId={}", reviewId, e);
        }
    }
}
