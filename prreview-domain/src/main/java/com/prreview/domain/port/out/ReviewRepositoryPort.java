package com.prreview.domain.port.out;

import com.prreview.domain.model.review.Review;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and retrieving Review aggregates.
 * Implemented by JpaReviewRepositoryAdapter in infrastructure.
 */
public interface ReviewRepositoryPort {

    /** Persists a new or updated review. */
    Review save(Review review);

    /** Finds a review by its ID. */
    Optional<Review> findById(UUID id);

    /** Finds a review by idempotency key (for deduplication). */
    Optional<Review> findByIdempotencyKey(String idempotencyKey);

    /** Checks whether a review with the given idempotency key exists. */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
