package com.prreview.domain.model.risk;

/**
 * Input signal to ConfidenceScoringService.
 * Aggregates all factors that influence confidence scoring.
 */
public record RiskSignal(
        Channels channels,
        boolean ruleHit,
        double aiSelfConfidence,
        RiskCategory category,
        boolean contextComplete,
        double feedbackCalibrationFactor) {

    public RiskSignal {
        if (aiSelfConfidence < 0.0 || aiSelfConfidence > 1.0) {
            throw new IllegalArgumentException("aiSelfConfidence must be between 0.0 and 1.0");
        }
        if (feedbackCalibrationFactor <= 0.0) {
            throw new IllegalArgumentException("feedbackCalibrationFactor must be positive");
        }
    }

    /** Creates a signal with default calibration factor (1.0 = no adjustment). */
    public static RiskSignal of(Channels channels, boolean ruleHit, double aiSelfConfidence,
                                RiskCategory category, boolean contextComplete) {
        return new RiskSignal(channels, ruleHit, aiSelfConfidence, category, contextComplete, 1.0);
    }
}
