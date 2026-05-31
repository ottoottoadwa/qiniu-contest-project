package com.prreview.infrastructure.ai;

import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.ChangeSummary;
import com.prreview.domain.model.review.ChangeType;
import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.ReviewSuggestion;
import com.prreview.domain.model.risk.Severity;
import com.prreview.domain.port.out.ChatModelPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Implements ChatModelPort using Spring AI ChatClient.
 * Routes between fast and slow model tiers based on analysis profile.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiChatModelAdapter implements ChatModelPort {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<AiRiskFinding> analyzeRisks(String renderedContext, String filePath,
                                             AnalysisProfile profile) {
        log.debug("Analyzing risks for file: {}, profile: {}", filePath, profile);
        try {
            String userPrompt = PromptTemplates.RISK_USER
                    .replace("{context}", truncate(renderedContext, 12000));

            String response = chatClient.prompt()
                    .system(PromptTemplates.RISK_SYSTEM)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseRiskFindings(response, filePath);
        } catch (Exception e) {
            log.error("Risk analysis failed for {}: {}", filePath, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public ChangeSummary summarizeChanges(String renderedContext, AnalysisProfile profile) {
        log.debug("Generating PR summary, profile: {}", profile);
        try {
            String userPrompt = PromptTemplates.SUMMARY_USER
                    .replace("{context}", truncate(renderedContext, 16000));

            String response = chatClient.prompt()
                    .system(PromptTemplates.SUMMARY_SYSTEM)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseSummary(response);
        } catch (Exception e) {
            log.error("Summary generation failed: {}", e.getMessage(), e);
            return new ChangeSummary("Analysis completed", "Unable to generate summary",
                    List.of(), ChangeType.FEATURE, List.of());
        }
    }

    @Override
    public ReviewSuggestion generateSuggestion(String riskItemId, String riskDescription,
                                                String codeContext) {
        log.debug("Generating suggestion for riskItem: {}", riskItemId);
        try {
            String userPrompt = PromptTemplates.SUGGESTION_USER
                    .replace("{riskDescription}", riskDescription)
                    .replace("{codeContext}", truncate(codeContext, 4000));

            String response = chatClient.prompt()
                    .system(PromptTemplates.SUGGESTION_SYSTEM)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseSuggestion(riskItemId, response);
        } catch (Exception e) {
            log.error("Suggestion generation failed for {}: {}", riskItemId, e.getMessage(), e);
            return new ReviewSuggestion(riskItemId,
                    "Unable to generate suggestion", riskDescription, null, List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private List<AiRiskFinding> parseRiskFindings(String response, String defaultFilePath) {
        try {
            String json = extractJson(response);
            List<Map<String, Object>> items = objectMapper.readValue(json,
                    new TypeReference<>() {});
            return items.stream()
                    .map(item -> new AiRiskFinding(
                            getString(item, "filePath", defaultFilePath),
                            getInt(item, "startLine", 1),
                            getInt(item, "endLine", 1),
                            parseEnum(RiskCategory.class, getString(item, "category", "CORRECTNESS")),
                            parseEnum(Severity.class, getString(item, "severity", "MEDIUM")),
                            getDouble(item, "selfConfidence", 0.5),
                            getString(item, "description", ""),
                            getString(item, "rationale", "")))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse risk findings JSON: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private ChangeSummary parseSummary(String response) {
        try {
            String json = extractJson(response);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new ChangeSummary(
                    getString(map, "headline", "PR analyzed"),
                    getString(map, "inferredPurpose", ""),
                    getList(map, "affectedModules"),
                    parseEnum(ChangeType.class, getString(map, "primaryType", "FEATURE")),
                    getList(map, "riskHighlights"));
        } catch (Exception e) {
            log.warn("Failed to parse summary JSON: {}", e.getMessage());
            return new ChangeSummary("PR analyzed", "", List.of(), ChangeType.FEATURE, List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private ReviewSuggestion parseSuggestion(String riskItemId, String response) {
        try {
            String json = extractJson(response);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new ReviewSuggestion(
                    riskItemId,
                    getString(map, "explanation", ""),
                    getString(map, "recommendation", ""),
                    (String) map.get("suggestedPatch"),
                    getList(map, "references"));
        } catch (Exception e) {
            log.warn("Failed to parse suggestion JSON: {}", e.getMessage());
            return new ReviewSuggestion(riskItemId, "", "See description", null, List.of());
        }
    }

    /** Extracts JSON from a response that may contain markdown code blocks. */
    private String extractJson(String response) {
        if (response == null) return "[]";
        String trimmed = response.trim();
        // Strip markdown code blocks if present
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() > maxChars ? text.substring(0, maxChars) + "\n[truncated]" : text;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val instanceof String s ? s : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (Exception e) {
            return enumClass.getEnumConstants()[0];
        }
    }
}
