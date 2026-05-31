package com.prreview.domain.port.out;

import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.RiskCategory;

import java.util.List;
import java.util.Set;

/**
 * Outbound port for the static rule engine.
 * Implemented by StaticRuleEngineAdapter in infrastructure.
 */
public interface RuleEnginePort {

    /**
     * Scans a code snippet for rule-based risk findings.
     *
     * @param filePath   path of the file being scanned
     * @param codeSnippet the code content (diff hunk + context)
     * @param categories  risk categories to scan for (empty = all)
     * @return list of rule-based findings
     */
    List<AiRiskFinding> scan(String filePath, String codeSnippet, Set<RiskCategory> categories);
}
