package com.prreview.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level application configuration properties.
 * Bound from application.yml under prreview.*
 */
@ConfigurationProperties(prefix = "prreview")
public record PrReviewProperties(
        String apiKey,
        int maxConcurrentReviews) {

    public PrReviewProperties {
        apiKey = apiKey != null ? apiKey : "change-me-in-production";
        maxConcurrentReviews = maxConcurrentReviews > 0 ? maxConcurrentReviews : 10;
    }
}
