package com.prreview.infrastructure.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for GitHub API access.
 * Bound from application.yml under prreview.github.*
 */
@ConfigurationProperties(prefix = "prreview.github")
public record GitHubProperties(
        String token,
        String apiBaseUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        int maxRetries,
        int maxFilesPerPr) {

    public GitHubProperties {
        apiBaseUrl = apiBaseUrl != null ? apiBaseUrl : "https://api.github.com";
        connectTimeoutSeconds = connectTimeoutSeconds > 0 ? connectTimeoutSeconds : 3;
        readTimeoutSeconds = readTimeoutSeconds > 0 ? readTimeoutSeconds : 10;
        maxRetries = maxRetries > 0 ? maxRetries : 3;
        maxFilesPerPr = maxFilesPerPr > 0 ? maxFilesPerPr : 300;
    }
}
