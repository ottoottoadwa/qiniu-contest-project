package com.prreview.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for model routing.
 * Bound from application.yml under prreview.model-routing.*
 */
@ConfigurationProperties(prefix = "prreview.model-routing")
public record ModelRoutingProperties(
        String fast,
        String slow,
        boolean cascadeEnabled,
        int perReviewTokenBudget,
        Duration llmTimeoutFast,
        Duration llmTimeoutSlow) {

    public ModelRoutingProperties {
        fast = fast != null ? fast : "openai";
        slow = slow != null ? slow : "openai";
        cascadeEnabled = cascadeEnabled;
        perReviewTokenBudget = perReviewTokenBudget > 0 ? perReviewTokenBudget : 200000;
        llmTimeoutFast = llmTimeoutFast != null ? llmTimeoutFast : Duration.ofSeconds(15);
        llmTimeoutSlow = llmTimeoutSlow != null ? llmTimeoutSlow : Duration.ofSeconds(40);
    }
}
