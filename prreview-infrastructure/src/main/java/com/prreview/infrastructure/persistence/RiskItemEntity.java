package com.prreview.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the risk_items table.
 */
@Entity
@Table(name = "risk_items")
@Getter
@Setter
@NoArgsConstructor
public class RiskItemEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewEntity review;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "confidence", nullable = false)
    private String confidence;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "detection_source", nullable = false)
    private String detectionSource;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "pattern_key")
    private String patternKey;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "rationale")
    private String rationale;

    @Column(name = "code_snippet")
    private String codeSnippet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "riskItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReviewSuggestionEntity suggestion;
}
