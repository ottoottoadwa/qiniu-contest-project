package com.prreview.web.dto;

import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.review.ReviewStatus;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.model.risk.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Complete review result response DTO.
 */
public record ReviewResultResponse(
        UUID reviewId,
        String repository,
        int pullRequestNumber,
        ReviewStatus status,
        ModelUsageDto modelUsage,
        ChangeSummaryDto summary,
        List<RiskItemDto> riskItems,
        ReviewMetricsDto metrics,
        OffsetDateTime completedAt) {

    /** Maps a Review domain object to this response DTO. */
    public static ReviewResultResponse from(Review review) {
        List<RiskItemDto> riskDtos = review.getRiskItems().stream()
                .map(ReviewResultResponse::toRiskItemDto)
                .toList();

        ChangeSummaryDto summaryDto = review.getSummary() != null
                ? new ChangeSummaryDto(
                        review.getSummary().headline(),
                        review.getSummary().inferredPurpose(),
                        review.getSummary().affectedModules(),
                        review.getSummary().primaryType(),
                        review.getSummary().riskHighlights())
                : null;

        Map<String, Integer> bySeverity = review.getRiskItems().stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.severity().name(),
                        Collectors.summingInt(ri -> 1)));

        Map<String, Integer> byCategory = review.getRiskItems().stream()
                .collect(Collectors.groupingBy(
                        ri -> ri.category().name(),
                        Collectors.summingInt(ri -> 1)));

        return new ReviewResultResponse(
                review.getId(),
                review.getRepository(),
                review.getPrNumber(),
                review.getStatus(),
                new ModelUsageDto("fast-model", "slow-model", 0, 0.0),
                summaryDto,
                riskDtos,
                new ReviewMetricsDto(bySeverity, byCategory),
                review.getCompletedAt());
    }

    private static RiskItemDto toRiskItemDto(RiskItem ri) {
        SuggestionDto suggestionDto = ri.suggestion() != null
                ? new SuggestionDto(
                        ri.suggestion().explanation(),
                        ri.suggestion().recommendation(),
                        ri.suggestion().suggestedPatch(),
                        ri.suggestion().references())
                : null;

        return new RiskItemDto(
                ri.id(), ri.filePath(), ri.startLine(), ri.endLine(),
                ri.category(), ri.severity(), ri.confidence(),
                ri.detectionSource(), ri.description(), ri.rationale(),
                suggestionDto);
    }
}
