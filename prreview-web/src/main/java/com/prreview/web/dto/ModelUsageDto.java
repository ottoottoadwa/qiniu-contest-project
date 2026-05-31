package com.prreview.web.dto;

/**
 * Response DTO for model usage statistics.
 */
public record ModelUsageDto(
        String fastModel,
        String slowModel,
        int totalTokens,
        double estimatedCostUsd) {}
