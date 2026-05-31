package com.prreview.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the review_suggestions table.
 * 1:1 with RiskItemEntity.
 */
@Entity
@Table(name = "review_suggestions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ReviewSuggestionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_item_id", nullable = false, unique = true)
    private RiskItemEntity riskItem;

    @Column(name = "explanation", nullable = false)
    private String explanation;

    @Column(name = "recommendation", nullable = false)
    private String recommendation;

    @Column(name = "suggested_patch")
    private String suggestedPatch;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "references", columnDefinition = "jsonb")
    private List<String> references;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
