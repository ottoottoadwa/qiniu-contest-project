package com.prreview.application.service;

import com.prreview.application.async.ReviewTaskRunner;
import com.prreview.application.port.in.SubmitReviewUseCase;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

/**
 * Implements SubmitReviewUseCase.
 * Handles idempotency, persists the review task, and triggers async execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitReviewService implements SubmitReviewUseCase {

    private final ReviewRepositoryPort reviewRepository;
    private final ReviewTaskRunner taskRunner;

    @Override
    @Transactional
    public ReviewAccepted submit(SubmitReviewCommand command, String idempotencyKey) {
        // Idempotency check: same key → return existing review
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Review> existing = reviewRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Review review = existing.get();
                log.info("Idempotent submit: reviewId={}, key={}", review.getId(), idempotencyKey);
                return new ReviewAccepted(review.getId(),
                        "/api/reviews/v1/" + review.getId() + "/status");
            }
        }

        // Create and persist the review task
        Review review = Review.create(
                command.repository(),
                command.pullRequestNumber(),
                command.analysisProfile(),
                idempotencyKey);
        review = reviewRepository.save(review);

        log.info("Review submitted: reviewId={}, repo={}, pr={}",
                review.getId(), command.repository(), command.pullRequestNumber());

        // Trigger async analysis (non-blocking)
        taskRunner.runAsync(review.getId(),
                new HashSet<>(command.riskCategories()));

        return new ReviewAccepted(review.getId(),
                "/api/reviews/v1/" + review.getId() + "/status");
    }
}
