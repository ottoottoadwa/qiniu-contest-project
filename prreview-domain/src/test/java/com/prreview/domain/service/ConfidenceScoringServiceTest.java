package com.prreview.domain.service;

import com.prreview.domain.model.risk.Channels;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfidenceScoringService.
 * Tests the confidence formula from docs/04 §5.2.
 */
class ConfidenceScoringServiceTest {

    private ConfidenceScoringService service;

    @BeforeEach
    void setUp() {
        service = new ConfidenceScoringService();
    }

    @Test
    @DisplayName("shouldReturnHighConfidence_whenBothChannelsHitSecurityCategory")
    void shouldReturnHighConfidence_whenBothChannelsHitSecurityCategory() {
        // Arrange
        RiskSignal signal = new RiskSignal(
                Channels.BOTH, true, 0.9,
                RiskCategory.SECURITY, true, 1.0);

        // Act
        Confidence result = service.score(signal);

        // Assert
        assertThat(result).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("shouldReturnHighConfidence_whenRuleOnlyHitsSecurityCategory")
    void shouldReturnHighConfidence_whenRuleOnlyHitsSecurityCategory() {
        // Arrange
        RiskSignal signal = new RiskSignal(
                Channels.RULE_ONLY, true, 0.0,
                RiskCategory.SECURITY, true, 1.0);

        // Act
        Confidence result = service.score(signal);

        // Assert
        // base=0.75, categoryWeight=1.0, contextPenalty=1.0, calibration=1.0 → 0.75 → HIGH
        assertThat(result).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("shouldReturnMediumConfidence_whenAiOnlyWithModerateConfidenceOnMaintainability")
    void shouldReturnMediumConfidence_whenAiOnlyWithModerateConfidenceOnMaintainability() {
        // Arrange
        RiskSignal signal = new RiskSignal(
                Channels.AI_ONLY, false, 0.7,
                RiskCategory.MAINTAINABILITY, true, 1.0);

        // Act
        Confidence result = service.score(signal);

        // Assert
        // base=0.7, categoryWeight=0.8, contextPenalty=1.0, calibration=1.0 → 0.56 → MEDIUM
        assertThat(result).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("shouldReturnLowConfidence_whenAiOnlyWithLowSelfConfidence")
    void shouldReturnLowConfidence_whenAiOnlyWithLowSelfConfidence() {
        // Arrange
        RiskSignal signal = new RiskSignal(
                Channels.AI_ONLY, false, 0.3,
                RiskCategory.MAINTAINABILITY, true, 1.0);

        // Act
        Confidence result = service.score(signal);

        // Assert
        // base=0.3, categoryWeight=0.8, contextPenalty=1.0, calibration=1.0 → 0.24 → LOW
        assertThat(result).isEqualTo(Confidence.LOW);
    }

    @Test
    @DisplayName("shouldApplyContextPenalty_whenContextIsIncomplete")
    void shouldApplyContextPenalty_whenContextIsIncomplete() {
        // Arrange — same signal but incomplete context
        RiskSignal withContext = RiskSignal.of(
                Channels.AI_ONLY, false, 0.6, RiskCategory.CORRECTNESS, true);
        RiskSignal withoutContext = RiskSignal.of(
                Channels.AI_ONLY, false, 0.6, RiskCategory.CORRECTNESS, false);

        // Act
        double scoreWith = service.rawScore(withContext);
        double scoreWithout = service.rawScore(withoutContext);

        // Assert — incomplete context should lower the score
        assertThat(scoreWithout).isLessThan(scoreWith);
    }

    @Test
    @DisplayName("shouldApplyFeedbackCalibration_whenCalibrationFactorIsLow")
    void shouldApplyFeedbackCalibration_whenCalibrationFactorIsLow() {
        // Arrange — calibration factor 0.5 (many false positives for this pattern)
        RiskSignal calibrated = new RiskSignal(
                Channels.BOTH, true, 0.9,
                RiskCategory.SECURITY, true, 0.5);
        RiskSignal uncalibrated = new RiskSignal(
                Channels.BOTH, true, 0.9,
                RiskCategory.SECURITY, true, 1.0);

        // Act
        double calibratedScore = service.rawScore(calibrated);
        double uncalibratedScore = service.rawScore(uncalibrated);

        // Assert — calibrated score should be lower
        assertThat(calibratedScore).isLessThan(uncalibratedScore);
    }

    @Test
    @DisplayName("shouldClampScoreToValidRange")
    void shouldClampScoreToValidRange() {
        // Arrange — extreme calibration factor
        RiskSignal signal = new RiskSignal(
                Channels.BOTH, true, 1.0,
                RiskCategory.SECURITY, true, 2.0);

        // Act
        double score = service.rawScore(signal);

        // Assert — score must be clamped to [0.0, 1.0]
        assertThat(score).isLessThanOrEqualTo(1.0);
        assertThat(score).isGreaterThanOrEqualTo(0.0);
    }
}
