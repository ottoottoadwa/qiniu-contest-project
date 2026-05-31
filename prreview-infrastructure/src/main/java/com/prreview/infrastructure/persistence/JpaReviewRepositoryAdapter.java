package com.prreview.infrastructure.persistence;

import com.prreview.domain.model.review.Review;
import com.prreview.domain.port.out.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implements ReviewRepositoryPort using Spring Data JPA.
 * Translates between domain objects and JPA entities via ReviewMapper.
 */
@Repository
@RequiredArgsConstructor
public class JpaReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final JpaReviewRepository jpaRepository;
    private final ReviewMapper mapper;

    @Override
    public Review save(Review review) {
        ReviewEntity entity = mapper.toEntity(review);
        ReviewEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Review> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }
}
