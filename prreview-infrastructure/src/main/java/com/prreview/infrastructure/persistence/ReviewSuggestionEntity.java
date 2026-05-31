package com.prreview.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the review_suggestions table.
 * 1:1 with RiskItemEntity.
 */
@Entity
@Table(name = "review_suggestions")
@Getter
@Setter
@NoArgsConstructor
public class ReviewSuggestionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
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
    @Column(name = "`references`", columnDefinition = "json")
    private List<String> references;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
