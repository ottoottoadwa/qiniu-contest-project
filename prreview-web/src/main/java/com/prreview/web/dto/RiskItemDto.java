package com.prreview.web.dto;

import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.DetectionSource;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.Severity;

/**
 * Response DTO for a single risk item.
 */
public record RiskItemDto(
        String riskItemId,
        String filePath,
        int startLine,
        int endLine,
        RiskCategory category,
        Severity severity,
        Confidence confidence,
        DetectionSource source,
        String description,
        String rationale,
        SuggestionDto suggestion) {}
