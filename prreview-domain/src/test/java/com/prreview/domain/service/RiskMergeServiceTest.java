package com.prreview.domain.service;

import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.Channels;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.DetectionSource;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.model.risk.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RiskMergeService.
 * Verifies cross-validation logic and confidence assignment.
 */
class RiskMergeServiceTest {

    private RiskMergeService mergeService;

    @BeforeEach
    void setUp() {
        mergeService = new RiskMergeService(new ConfidenceScoringService());
    }

    @Test
    @DisplayName("shouldProduceBothSourceItem_whenRuleAndAiBothHitSameLocation")
    void shouldProduceBothSourceItem_whenRuleAndAiBothHitSameLocation() {
        // Arrange
        AiRiskFinding ruleFinding = new AiRiskFinding(
                "Foo.java", 10, 10, RiskCategory.SECURITY, Severity.HIGH, 0.9,
                "SQL injection", "String concat in query");
        AiRiskFinding aiFinding = new AiRiskFinding(
                "Foo.java", 10, 12, RiskCategory.SECURITY, Severity.HIGH, 0.85,
                "SQL injection detected", "User input concatenated");

        // Act
        List<RiskItem> result = mergeService.merge(
                "review-1", List.of(ruleFinding), List.of(aiFinding), true, Map.of());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).detectionSource()).isEqualTo(DetectionSource.BOTH);
        assertThat(result.get(0).confidence()).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("shouldProduceRuleSourceItem_whenOnlyRuleHits")
    void shouldProduceRuleSourceItem_whenOnlyRuleHits() {
        // Arrange
        AiRiskFinding ruleFinding = new AiRiskFinding(
                "Bar.java", 5, 5, RiskCategory.CORRECTNESS, Severity.MEDIUM, 0.9,
                "Empty catch block", "Exception swallowed");

        // Act
        List<RiskItem> result = mergeService.merge(
                "review-1", List.of(ruleFinding), List.of(), true, Map.of());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).detectionSource()).isEqualTo(DetectionSource.RULE);
    }

    @Test
    @DisplayName("shouldProduceAiSourceItem_whenOnlyAiHits")
    void shouldProduceAiSourceItem_whenOnlyAiHits() {
        // Arrange
        AiRiskFinding aiFinding = new AiRiskFinding(
                "Baz.java", 20, 25, RiskCategory.PERFORMANCE, Severity.MEDIUM, 0.7,
                "N+1 query", "Loop with repository call");

        // Act
        List<RiskItem> result = mergeService.merge(
                "review-1", List.of(), List.of(aiFinding), true, Map.of());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).detectionSource()).isEqualTo(DetectionSource.AI);
    }

    @Test
    @DisplayName("shouldReturnEmptyList_whenNoFindings")
    void shouldReturnEmptyList_whenNoFindings() {
        // Act
        List<RiskItem> result = mergeService.merge(
                "review-1", List.of(), List.of(), true, Map.of());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("shouldApplyCalibrationFactor_whenPatternHasManyFalsePositives")
    void shouldApplyCalibrationFactor_whenPatternHasManyFalsePositives() {
        // Arrange
        AiRiskFinding aiFinding = new AiRiskFinding(
                "Test.java", 1, 1, RiskCategory.MAINTAINABILITY, Severity.LOW, 0.6,
                "Field injection", "@Autowired on field");

        // Calibration factor 0.3 — this pattern has many false positives
        Map<String, Double> calibration = Map.of("MAINTAINABILITY.java", 0.3);

        // Act
        List<RiskItem> result = mergeService.merge(
                "review-1", List.of(), List.of(aiFinding), true, calibration);

        // Assert — low calibration should result in LOW confidence
        assertThat(result).hasSize(1);
        assertThat(result.get(0).confidence()).isEqualTo(Confidence.LOW);
    }
}
