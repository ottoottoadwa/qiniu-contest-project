package com.prreview.domain.service;

import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.Channels;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.DetectionSource;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.model.risk.RiskSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure domain service that cross-validates rule engine findings with AI findings
 * and produces a merged, confidence-scored list of RiskItems.
 *
 * <p>Fusion rules (from docs/04 §5.1):
 * <ul>
 *   <li>Rule + AI both hit same location/category → HIGH confidence (BOTH source)</li>
 *   <li>Rule only → MEDIUM/HIGH confidence (RULE source)</li>
 *   <li>AI only → confidence from AI self-score (AI source)</li>
 * </ul>
 */
public class RiskMergeService {

    private final ConfidenceScoringService confidenceScoring;

    public RiskMergeService(ConfidenceScoringService confidenceScoring) {
        this.confidenceScoring = confidenceScoring;
    }

    /**
     * Merges rule findings and AI findings into a unified list of RiskItems.
     *
     * @param reviewId       ID of the parent review
     * @param ruleFindings   findings from the rule engine
     * @param aiFindings     findings from the AI model
     * @param contextComplete whether full L1/L2 context was available
     * @param calibrationMap  pattern key → calibration factor (1.0 = no adjustment)
     * @return merged, confidence-scored risk items
     */
    public List<RiskItem> merge(String reviewId,
                                List<AiRiskFinding> ruleFindings,
                                List<AiRiskFinding> aiFindings,
                                boolean contextComplete,
                                Map<String, Double> calibrationMap) {
        List<RiskItem> result = new ArrayList<>();

        // Index AI findings by a location+category key for overlap detection
        Map<String, AiRiskFinding> aiByKey = aiFindings.stream()
                .collect(Collectors.toMap(
                        f -> locationKey(f.filePath(), f.startLine(), f.category()),
                        Function.identity(),
                        (a, b) -> a)); // keep first on collision

        // Process rule findings — check for AI overlap
        for (AiRiskFinding ruleFinding : ruleFindings) {
            String key = locationKey(ruleFinding.filePath(), ruleFinding.startLine(), ruleFinding.category());
            AiRiskFinding aiMatch = aiByKey.remove(key); // remove so we don't double-count

            Channels channels = aiMatch != null ? Channels.BOTH : Channels.RULE_ONLY;
            double aiSelfConf = aiMatch != null ? aiMatch.selfConfidence() : 0.75;
            String patternKey = buildPatternKey(ruleFinding.category(), ruleFinding.filePath());
            double calibration = calibrationMap.getOrDefault(patternKey, 1.0);

            RiskSignal signal = new RiskSignal(channels, true, aiSelfConf,
                    ruleFinding.category(), contextComplete, calibration);
            double rawScore = confidenceScoring.rawScore(signal);
            Confidence confidence = confidenceScoring.score(signal);

            result.add(RiskItem.create(
                    reviewId, ruleFinding.filePath(),
                    ruleFinding.startLine(), ruleFinding.endLine(),
                    ruleFinding.category(), ruleFinding.severity(),
                    confidence, rawScore,
                    channels == Channels.BOTH ? DetectionSource.BOTH : DetectionSource.RULE,
                    null, patternKey,
                    ruleFinding.description(), ruleFinding.rationale()));
        }

        // Process remaining AI-only findings
        for (AiRiskFinding aiFinding : aiByKey.values()) {
            String patternKey = buildPatternKey(aiFinding.category(), aiFinding.filePath());
            double calibration = calibrationMap.getOrDefault(patternKey, 1.0);

            RiskSignal signal = new RiskSignal(Channels.AI_ONLY, false, aiFinding.selfConfidence(),
                    aiFinding.category(), contextComplete, calibration);
            double rawScore = confidenceScoring.rawScore(signal);
            Confidence confidence = confidenceScoring.score(signal);

            result.add(RiskItem.create(
                    reviewId, aiFinding.filePath(),
                    aiFinding.startLine(), aiFinding.endLine(),
                    aiFinding.category(), aiFinding.severity(),
                    confidence, rawScore,
                    DetectionSource.AI,
                    null, patternKey,
                    aiFinding.description(), aiFinding.rationale()));
        }

        return result;
    }

    private String locationKey(String filePath, int startLine, RiskCategory category) {
        return filePath + ":" + startLine + ":" + category.name();
    }

    private String buildPatternKey(RiskCategory category, String filePath) {
        // Simple pattern key: category + file extension
        String ext = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.')) : "";
        return category.name() + ext;
    }
}
