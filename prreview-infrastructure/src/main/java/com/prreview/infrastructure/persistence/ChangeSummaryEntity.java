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
 * JPA entity for the change_summaries table.
 * 1:1 with ReviewEntity.
 */
@Entity
@Table(name = "change_summaries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ChangeSummaryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false, unique = true)
    private ReviewEntity review;

    @Column(name = "headline", nullable = false)
    private String headline;

    @Column(name = "inferred_purpose")
    private String inferredPurpose;

    @Column(name = "primary_type")
    private String primaryType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_modules", columnDefinition = "json")
    private List<String> affectedModules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_highlights", columnDefinition = "json")
    private List<String> riskHighlights;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
