package com.prreview.infrastructure.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Direct HTTP client for Qwen API with proper timeout configuration.
 * Uses a singleton RestClient to avoid connection pool exhaustion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectQwenClient {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:qwen-plus}")
    private String model;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("DirectQwenClient initialized with baseUrl: {}", baseUrl);
    }

    public String callWithSystemAndUser(String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            log.info("========== QWEN API CALL ==========");
            log.info("System prompt length: {}", systemPrompt.length());
            log.info("User prompt length: {}", userPrompt.length());
            log.info("System prompt preview: {}", systemPrompt.substring(0, Math.min(200, systemPrompt.length())));
            log.info("User prompt preview: {}", userPrompt.substring(0, Math.min(500, userPrompt.length())));

            log.debug("Calling Qwen API: {}/v1/chat/completions", baseUrl);
            Map<String, Object> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            // Extract content from response
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                String content = (String) message.get("content");

                log.info("========== QWEN API RESPONSE ==========");
                log.info("Response length: {}", content.length());
                log.info("Full response content:\n{}", content);
                log.info("======================================");

                return content;
            }

            log.warn("No choices in API response");
            return "";
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Qwen API call failed", e);
        }
    }

    public String testCall(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            log.info("Testing direct API call to: {}/v1/chat/completions", baseUrl);
            Map<String, Object> response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            log.info("API call successful: {}", response);
            return response.toString();
        } catch (Exception e) {
            log.error("API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Direct API call failed", e);
        }
    }
}
