package com.prreview.web.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.prreview.infrastructure.github.GitHubProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Infrastructure bean configuration.
 * Wires RestClient for GitHub API, Caffeine cache, and Spring AI ChatClient.
 */
@Configuration
@EnableConfigurationProperties({GitHubProperties.class,
        com.prreview.infrastructure.ai.ModelRoutingProperties.class})
public class InfrastructureConfig {

    /**
     * ClientHttpRequestFactory with timeout for all RestClient instances.
     * Spring AI will use this factory when creating its internal RestClient.
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofSeconds(60));
        return ClientHttpRequestFactories.get(settings);
    }

    /**
     * RestClient configured for GitHub API.
     * Uses PAT authentication for MVP; GitHub App tokens for production.
     */
    @Bean
    public RestClient gitHubRestClient(GitHubProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Caffeine in-process cache for diff results and analysis results.
     * TTL: 1 hour. Max size: 500 entries.
     * Production: replace with Redis for multi-instance deployments.
     */
    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(500)
                .recordStats()
                .build();
    }

    /**
     * Spring AI ChatClient for LLM interactions.
     * Configured with OpenAI-compatible endpoint (Alibaba DashScope).
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
