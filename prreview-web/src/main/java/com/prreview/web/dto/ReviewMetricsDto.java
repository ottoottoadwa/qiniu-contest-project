package com.prreview.web.dto;

import java.util.Map;

/**
 * Response DTO for review metrics summary.
 */
public record ReviewMetricsDto(
        Map<String, Integer> riskCountBySeverity,
        Map<String, Integer> riskCountByCategory) {}
