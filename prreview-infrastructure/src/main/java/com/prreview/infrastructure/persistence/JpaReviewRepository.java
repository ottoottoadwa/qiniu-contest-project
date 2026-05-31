package com.prreview.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ReviewEntity.
 * Custom queries use JPQL with named parameters — no string concatenation.
 */
public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    /** Finds a review by idempotency key (for deduplication). */
    @Query("SELECT r FROM ReviewEntity r WHERE r.idempotencyKey = :key AND r.deleted = false")
    Optional<ReviewEntity> findByIdempotencyKey(@Param("key") String idempotencyKey);

    /** Checks whether a review with the given idempotency key exists. */
    @Query("SELECT COUNT(r) > 0 FROM ReviewEntity r WHERE r.idempotencyKey = :key AND r.deleted = false")
    boolean existsByIdempotencyKey(@Param("key") String idempotencyKey);

    /** Lists reviews for a repository, newest first, paginated. */
    @Query("SELECT r FROM ReviewEntity r WHERE r.repository = :repo AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<ReviewEntity> findByRepository(@Param("repo") String repository, Pageable pageable);
}
