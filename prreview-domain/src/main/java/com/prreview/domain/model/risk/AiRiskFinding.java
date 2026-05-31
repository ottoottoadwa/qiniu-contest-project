package com.prreview.domain.model.risk;

/**
 * A raw risk finding produced by the AI analysis channel.
 * Input to the RiskMergeService for cross-validation with rule findings.
 */
public record AiRiskFinding(
        String filePath,
        int startLine,
        int endLine,
        RiskCategory category,
        Severity severity,
        double selfConfidence,
        String description,
        String rationale) {

    public AiRiskFinding {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("AiRiskFinding filePath must not be blank");
        }
        if (selfConfidence < 0.0 || selfConfidence > 1.0) {
            throw new IllegalArgumentException("selfConfidence must be between 0.0 and 1.0");
        }
    }
}
