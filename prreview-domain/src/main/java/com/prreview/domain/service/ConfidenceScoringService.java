package com.prreview.domain.service;

import com.prreview.domain.model.risk.Channels;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskSignal;

/**
 * Pure domain service that computes a confidence score for a risk signal.
 * Formula from docs/04 §5.2. Zero framework dependencies — fully unit-testable.
 */
public class ConfidenceScoringService {

    /**
     * Computes a Confidence level from a RiskSignal.
     *
     * <p>Scoring formula:
     * <ol>
     *   <li>Base score: BOTH channels = 0.9, rule-only = 0.75, AI-only = AI self-confidence</li>
     *   <li>Category weight: security/correctness = 1.0, others = 0.8</li>
     *   <li>Context penalty: incomplete context = 0.85 multiplier</li>
     *   <li>Feedback calibration: pattern-level adjustment factor</li>
     * </ol>
     */
    public Confidence score(RiskSignal signal) {
        double base = computeBase(signal);
        double categoryWeight = signal.category().isSecurityOrCorrectness() ? 1.0 : 0.8;
        double contextPenalty = signal.contextComplete() ? 1.0 : 0.85;
        double calibrated = base * categoryWeight * contextPenalty * signal.feedbackCalibrationFactor();

        // Clamp to [0.0, 1.0]
        calibrated = Math.max(0.0, Math.min(1.0, calibrated));
        return Confidence.fromScore(calibrated);
    }

    /**
     * Returns the raw calibrated score (0.0–1.0) for storage and threshold tuning.
     */
    public double rawScore(RiskSignal signal) {
        double base = computeBase(signal);
        double categoryWeight = signal.category().isSecurityOrCorrectness() ? 1.0 : 0.8;
        double contextPenalty = signal.contextComplete() ? 1.0 : 0.85;
        double calibrated = base * categoryWeight * contextPenalty * signal.feedbackCalibrationFactor();
        return Math.max(0.0, Math.min(1.0, calibrated));
    }

    private double computeBase(RiskSignal signal) {
        if (signal.channels() == Channels.BOTH) { // NOSONAR: intentional enum comparison
            return 0.9;
        }
        if (signal.ruleHit()) {
            return 0.75;
        }
        return signal.aiSelfConfidence();
    }
}
