package com.prreview.application.service;

import com.prreview.application.port.in.GetReviewQuery;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.review.ReviewStatus;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implements GetReviewQuery.
 * Provides status polling and result retrieval for review tasks.
 */
@Service
@RequiredArgsConstructor
public class GetReviewService implements GetReviewQuery {

    private final ReviewRepositoryPort reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewStatusView status(UUID reviewId) {
        Review review = findOrThrow(reviewId);

        String resultUrl = review.isCompleted()
                ? "/api/reviews/v1/" + reviewId
                : null;

        // Simple remaining time estimate: assume 30s total, scale by progress
        Integer estimatedRemaining = null;
        if (review.getStatus() == ReviewStatus.RUNNING && review.getProgress() > 0) {
            long elapsed = review.getStartedAt() != null
                    ? java.time.Duration.between(review.getStartedAt(),
                            java.time.OffsetDateTime.now()).getSeconds()
                    : 0;
            double remaining = elapsed / review.getProgress() * (1 - review.getProgress());
            estimatedRemaining = (int) Math.max(1, remaining);
        }

        return new ReviewStatusView(
                review.getId(),
                review.getStatus(),
                review.getProgress(),
                review.getFilesTotal(),
                review.getFilesAnalyzed(),
                review.getStartedAt(),
                estimatedRemaining,
                resultUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Review result(UUID reviewId) {
        Review review = findOrThrow(reviewId);
        if (!review.isCompleted()) {
            throw new ReviewNotReadyException(reviewId);
        }
        return review;
    }

    private Review findOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    /** Thrown when a review is not found. */
    public static class ReviewNotFoundException extends RuntimeException {
        public ReviewNotFoundException(UUID reviewId) {
            super("Review not found: " + reviewId);
        }
    }

    /** Thrown when a review result is requested but the review is not yet complete. */
    public static class ReviewNotReadyException extends RuntimeException {
        public ReviewNotReadyException(UUID reviewId) {
            super("Review not yet completed: " + reviewId);
        }
    }
}
