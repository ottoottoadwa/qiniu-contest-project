package com.prreview.domain.port.out;

import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.ChangeSummary;
import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.ReviewSuggestion;

import java.util.List;

/**
 * Outbound port for AI model interactions.
 * Implemented by SpringAiChatModelAdapter in infrastructure.
 * Domain layer defines the capability contract; infrastructure provides the implementation.
 */
public interface ChatModelPort {

    /**
     * Analyzes a rendered context package for risk findings.
     *
     * @param renderedContext the assembled context (diff + related code) as a string
     * @param filePath        the file being analyzed (for logging/routing)
     * @param profile         analysis depth profile for model routing
     * @return list of AI-identified risk findings
     */
    List<AiRiskFinding> analyzeRisks(String renderedContext, String filePath, AnalysisProfile profile);

    /**
     * Generates a structured change summary for the entire PR.
     *
     * @param renderedContext aggregated PR context (metadata + file summaries)
     * @param profile         analysis depth profile
     * @return structured change summary
     */
    ChangeSummary summarizeChanges(String renderedContext, AnalysisProfile profile);

    /**
     * Generates a concrete review suggestion for a specific risk item.
     *
     * @param riskItemId      ID of the risk item to generate suggestion for
     * @param riskDescription description of the risk
     * @param codeContext     relevant code snippet and context
     * @return actionable review suggestion
     */
    ReviewSuggestion generateSuggestion(String riskItemId, String riskDescription, String codeContext);
}
