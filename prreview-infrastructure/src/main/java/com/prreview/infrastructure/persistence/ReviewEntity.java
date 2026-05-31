package com.prreview.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the reviews table.
 * Maps to the aggregate root Review domain object.
 */
@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "repository", nullable = false)
    private String repository;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "pr_head_sha")
    private String prHeadSha;

    @Column(name = "pr_title")
    private String prTitle;

    @Column(name = "pr_author")
    private String prAuthor;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "analysis_profile", nullable = false)
    private String analysisProfile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_categories", columnDefinition = "json")
    private List<String> requestedCategories;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "progress", nullable = false)
    private Double progress;

    @Column(name = "files_total", nullable = false)
    private int filesTotal;

    @Column(name = "files_analyzed", nullable = false)
    private int filesAnalyzed;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // Summary stored as embedded JSON in a separate table (1:1)
    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ChangeSummaryEntity summary;

    // Risk items (1:many, lazy)
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RiskItemEntity> riskItems;
}
