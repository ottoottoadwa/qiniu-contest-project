package com.prreview.web.controller;

import com.prreview.infrastructure.ai.DirectQwenClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Debug endpoint to verify AI configuration.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    @Value("${spring.ai.openai.api-key:NOT_SET}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:NOT_SET}")
    private String baseUrl;

    private final ChatClient chatClient;
    private final DirectQwenClient directQwenClient;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
                "apiKeySet", apiKey.equals("NOT_SET") ? "NO" : "YES (length: " + apiKey.length() + ")",
                "baseUrl", baseUrl
        );
    }

    @GetMapping("/test-ai")
    public Map<String, String> testAi() {
        try {
            String response = chatClient.prompt()
                    .user("Say 'Hello' in one word")
                    .call()
                    .content();
            return Map.of("status", "SUCCESS", "response", response);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage());
        }
    }

    @GetMapping("/test-direct")
    public Map<String, String> testDirect() {
        try {
            String response = directQwenClient.testCall("Say 'Hello' in one word");
            return Map.of("status", "SUCCESS", "response", response);
        } catch (Exception e) {
            return Map.of("status", "ERROR", "error", e.getMessage(), "cause", e.getCause() != null ? e.getCause().getMessage() : "none");
        }
    }
}
