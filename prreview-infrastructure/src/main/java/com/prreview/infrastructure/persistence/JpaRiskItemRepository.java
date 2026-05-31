package com.prreview.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for RiskItemEntity.
 */
public interface JpaRiskItemRepository extends JpaRepository<RiskItemEntity, UUID> {

    /** Finds all risk items for a review, ordered by severity. */
    List<RiskItemEntity> findByReviewIdOrderBySeverityAscConfidenceAsc(UUID reviewId);
}
