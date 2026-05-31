package com.prreview.infrastructure.persistence;

import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.ChangeSummary;
import com.prreview.domain.model.review.ChangeType;
import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.review.ReviewStatus;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.DetectionSource;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.model.risk.ReviewSuggestion;
import com.prreview.domain.model.risk.Severity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Maps between domain objects and JPA entities.
 * No Lombok @Data on entities — explicit mapping prevents accidental mutation.
 */
@Component
public class ReviewMapper {

    /** Maps a Review domain object to a ReviewEntity for persistence. */
    public ReviewEntity toEntity(Review review) {
        ReviewEntity entity = new ReviewEntity();
        entity.setId(review.getId());
        entity.setRepository(review.getRepository());
        entity.setPrNumber(review.getPrNumber());
        entity.setStatus(review.getStatus().name());
        entity.setAnalysisProfile(review.getAnalysisProfile().name());
        entity.setIdempotencyKey(review.getIdempotencyKey());
        entity.setProgress(review.getProgress());
        entity.setFilesTotal(review.getFilesTotal());
        entity.setFilesAnalyzed(review.getFilesAnalyzed());
        entity.setFailureReason(review.getFailureReason());
        entity.setStartedAt(review.getStartedAt());
        entity.setCompletedAt(review.getCompletedAt());
        entity.setDeleted(false);

        if (review.getSummary() != null) {
            ChangeSummaryEntity summaryEntity = toSummaryEntity(review.getSummary(), entity);
            entity.setSummary(summaryEntity);
        }

        if (review.getRiskItems() != null && !review.getRiskItems().isEmpty()) {
            List<RiskItemEntity> riskEntities = review.getRiskItems().stream()
                    .map(ri -> toRiskItemEntity(ri, entity))
                    .toList();
            entity.setRiskItems(riskEntities);
        }

        return entity;
    }

    /** Maps a ReviewEntity back to a Review domain object. */
    public Review toDomain(ReviewEntity entity) {
        ChangeSummary summary = entity.getSummary() != null
                ? toSummaryDomain(entity.getSummary())
                : null;

        List<RiskItem> riskItems = entity.getRiskItems() != null
                ? entity.getRiskItems().stream().map(this::toRiskItemDomain).toList()
                : List.of();

        return Review.reconstitute(
                entity.getId(),
                entity.getRepository(),
                entity.getPrNumber(),
                parseEnum(AnalysisProfile.class, entity.getAnalysisProfile(), AnalysisProfile.STANDARD),
                entity.getIdempotencyKey(),
                parseEnum(ReviewStatus.class, entity.getStatus(), ReviewStatus.PENDING),
                entity.getProgress(),
                entity.getFilesTotal(),
                entity.getFilesAnalyzed(),
                entity.getFailureReason(),
                summary,
                riskItems,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt());
    }

    private ChangeSummaryEntity toSummaryEntity(ChangeSummary summary, ReviewEntity review) {
        ChangeSummaryEntity entity = new ChangeSummaryEntity();
        entity.setId(UUID.randomUUID());
        entity.setReview(review);
        entity.setHeadline(summary.headline());
        entity.setInferredPurpose(summary.inferredPurpose());
        entity.setPrimaryType(summary.primaryType() != null ? summary.primaryType().name() : null);
        entity.setAffectedModules(summary.affectedModules());
        entity.setRiskHighlights(summary.riskHighlights());
        return entity;
    }

    private ChangeSummary toSummaryDomain(ChangeSummaryEntity entity) {
        return new ChangeSummary(
                entity.getHeadline(),
                entity.getInferredPurpose(),
                entity.getAffectedModules() != null ? entity.getAffectedModules() : List.of(),
                parseEnum(ChangeType.class, entity.getPrimaryType(), ChangeType.FEATURE),
                entity.getRiskHighlights() != null ? entity.getRiskHighlights() : List.of());
    }

    private RiskItemEntity toRiskItemEntity(RiskItem riskItem, ReviewEntity review) {
        RiskItemEntity entity = new RiskItemEntity();
        entity.setId(UUID.fromString(riskItem.id()));
        entity.setReview(review);
        entity.setFilePath(riskItem.filePath());
        entity.setStartLine(riskItem.startLine());
        entity.setEndLine(riskItem.endLine());
        entity.setCategory(riskItem.category().name());
        entity.setSeverity(riskItem.severity().name());
        entity.setConfidence(riskItem.confidence().name());
        entity.setConfidenceScore(riskItem.confidenceScore());
        entity.setDetectionSource(riskItem.detectionSource().name());
        entity.setRuleId(riskItem.ruleId());
        entity.setPatternKey(riskItem.patternKey());
        entity.setDescription(riskItem.description());
        entity.setRationale(riskItem.rationale());

        if (riskItem.suggestion() != null) {
            ReviewSuggestionEntity suggEntity = toSuggestionEntity(riskItem.suggestion(), entity);
            entity.setSuggestion(suggEntity);
        }

        return entity;
    }

    private RiskItem toRiskItemDomain(RiskItemEntity entity) {
        ReviewSuggestion suggestion = entity.getSuggestion() != null
                ? toSuggestionDomain(entity.getSuggestion())
                : null;

        return new RiskItem(
                entity.getId().toString(),
                entity.getReview().getId().toString(),
                entity.getFilePath(),
                entity.getStartLine() != null ? entity.getStartLine() : 0,
                entity.getEndLine() != null ? entity.getEndLine() : 0,
                parseEnum(RiskCategory.class, entity.getCategory(), RiskCategory.CORRECTNESS),
                parseEnum(Severity.class, entity.getSeverity(), Severity.MEDIUM),
                parseEnum(Confidence.class, entity.getConfidence(), Confidence.MEDIUM),
                entity.getConfidenceScore() != null ? entity.getConfidenceScore() : 0.5,
                parseEnum(DetectionSource.class, entity.getDetectionSource(), DetectionSource.AI),
                entity.getRuleId(),
                entity.getPatternKey(),
                entity.getDescription(),
                entity.getRationale(),
                suggestion);
    }

    private ReviewSuggestionEntity toSuggestionEntity(ReviewSuggestion suggestion,
                                                        RiskItemEntity riskItem) {
        ReviewSuggestionEntity entity = new ReviewSuggestionEntity();
        entity.setId(UUID.randomUUID());
        entity.setRiskItem(riskItem);
        entity.setExplanation(suggestion.explanation());
        entity.setRecommendation(suggestion.recommendation());
        entity.setSuggestedPatch(suggestion.suggestedPatch());
        entity.setReferences(suggestion.references());
        return entity;
    }

    private ReviewSuggestion toSuggestionDomain(ReviewSuggestionEntity entity) {
        return new ReviewSuggestion(
                entity.getRiskItem().getId().toString(),
                entity.getExplanation(),
                entity.getRecommendation(),
                entity.getSuggestedPatch(),
                entity.getReferences() != null ? entity.getReferences() : List.of());
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
