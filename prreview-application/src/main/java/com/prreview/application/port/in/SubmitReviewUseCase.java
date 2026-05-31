package com.prreview.application.port.in;

import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.risk.RiskCategory;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for submitting a new PR review task.
 * Implemented by SubmitReviewService in the application layer.
 */
public interface SubmitReviewUseCase {

    /**
     * Submits a PR review task asynchronously.
     *
     * @param command      the review submission command
     * @param idempotencyKey optional idempotency key for safe retries
     * @return accepted response with the review ID and status URL
     */
    ReviewAccepted submit(SubmitReviewCommand command, String idempotencyKey);

    /** Command object for submitting a review. */
    record SubmitReviewCommand(
            String repository,
            int pullRequestNumber,
            AnalysisProfile analysisProfile,
            List<RiskCategory> riskCategories,
            String callbackUrl) {

        public SubmitReviewCommand {
            if (repository == null || repository.isBlank()) {
                throw new IllegalArgumentException("repository must not be blank");
            }
            if (pullRequestNumber <= 0) {
                throw new IllegalArgumentException("pullRequestNumber must be positive");
            }
            analysisProfile = analysisProfile != null ? analysisProfile : AnalysisProfile.STANDARD;
            riskCategories = riskCategories == null ? List.of() : List.copyOf(riskCategories);
        }
    }

    /** Response returned immediately after accepting the task. */
    record ReviewAccepted(UUID reviewId, String statusUrl) {}
}
