package com.prreview.application.review;

import com.prreview.application.context.ContextPackage;
import com.prreview.domain.model.review.AnalysisProfile;
import com.prreview.domain.model.review.ChangeSummary;
import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.Confidence;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.port.out.ChatModelPort;
import com.prreview.domain.port.out.RuleEnginePort;
import com.prreview.domain.service.ConfidenceScoringService;
import com.prreview.domain.service.RiskMergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Orchestrates the three analysis tasks: summary, risk identification, and suggestion generation.
 * Uses virtual threads for file-level parallelism.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisEngine {

    private final ChatModelPort chatModelPort;
    private final RuleEnginePort ruleEnginePort;
    private final RiskMergeService riskMergeService;

    /**
     * Runs all three analysis tasks for a PR.
     *
     * @param reviewId         ID of the review being analyzed
     * @param contextPackages  assembled context packages, one per file
     * @param profile          analysis depth profile
     * @param categories       risk categories to analyze (empty = all)
     * @param calibrationMap   pattern key → calibration factor
     * @return analysis result containing summary and risk items
     */
    public AnalysisResult analyze(String reviewId,
                                  List<ContextPackage> contextPackages,
                                  AnalysisProfile profile,
                                  Set<RiskCategory> categories,
                                  Map<String, Double> calibrationMap) {
        log.info("Starting analysis: reviewId={}, files={}, profile={}",
                reviewId, contextPackages.size(), profile);

        // Task 1: PR summary (runs concurrently with file analysis)
        String aggregatedContext = buildAggregatedContext(contextPackages);
        CompletableFuture<ChangeSummary> summaryFuture = CompletableFuture.supplyAsync(
                () -> chatModelPort.summarizeChanges(aggregatedContext, profile));

        // Task 2: File-level risk identification (parallel via virtual threads)
        List<RiskItem> allRiskItems = analyzeFilesInParallel(
                reviewId, contextPackages, profile, categories, calibrationMap);

        // Task 3: Suggestion generation for medium/high confidence items
        List<RiskItem> itemsWithSuggestions = generateSuggestions(allRiskItems, contextPackages);

        // Wait for summary
        ChangeSummary summary;
        try {
            summary = summaryFuture.get();
        } catch (Exception e) {
            log.error("Summary generation failed for reviewId={}: {}", reviewId, e.getMessage(), e);
            summary = new ChangeSummary("Analysis completed", "Unable to generate summary",
                    List.of(), com.prreview.domain.model.review.ChangeType.FEATURE, List.of());
        }

        log.info("Analysis complete: reviewId={}, riskItems={}", reviewId, itemsWithSuggestions.size());
        return new AnalysisResult(summary, itemsWithSuggestions);
    }

    private List<RiskItem> analyzeFilesInParallel(String reviewId,
                                                   List<ContextPackage> contextPackages,
                                                   AnalysisProfile profile,
                                                   Set<RiskCategory> categories,
                                                   Map<String, Double> calibrationMap) {
        // Virtual threads: one per file, all run concurrently
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<List<RiskItem>>> futures = contextPackages.stream()
                    .map(ctx -> CompletableFuture.supplyAsync(
                            () -> analyzeFile(reviewId, ctx, profile, categories, calibrationMap),
                            executor))
                    .toList();

            return futures.stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            log.error("File analysis failed: {}", e.getMessage(), e);
                            return List.<RiskItem>of();
                        }
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

    private List<RiskItem> analyzeFile(String reviewId, ContextPackage ctx,
                                        AnalysisProfile profile,
                                        Set<RiskCategory> categories,
                                        Map<String, Double> calibrationMap) {
        log.debug("Analyzing file: {}", ctx.filePath());
        String renderedContext = ctx.render();
        boolean contextComplete = !ctx.enclosingDefinitions().isBlank();

        // Rule engine channel
        List<AiRiskFinding> ruleFindings;
        try {
            ruleFindings = ruleEnginePort.scan(ctx.filePath(), renderedContext,
                    categories.isEmpty() ? Set.of(RiskCategory.values()) : categories);
        } catch (Exception e) {
            log.warn("Rule engine failed for {}: {}", ctx.filePath(), e.getMessage());
            ruleFindings = List.of();
        }

        // AI channel
        List<AiRiskFinding> aiFindings;
        try {
            aiFindings = chatModelPort.analyzeRisks(renderedContext, ctx.filePath(), profile);
        } catch (Exception e) {
            log.warn("AI analysis failed for {}: {}", ctx.filePath(), e.getMessage());
            aiFindings = List.of();
        }

        // Cross-validate and score
        return riskMergeService.merge(reviewId, ruleFindings, aiFindings,
                contextComplete, calibrationMap);
    }

    private List<RiskItem> generateSuggestions(List<RiskItem> riskItems,
                                                List<ContextPackage> contextPackages) {
        // Only generate suggestions for MEDIUM and HIGH confidence items
        Map<String, ContextPackage> ctxByFile = contextPackages.stream()
                .collect(Collectors.toMap(ContextPackage::filePath, c -> c, (a, b) -> a));

        return riskItems.stream()
                .map(item -> {
                    if (item.confidence() == Confidence.LOW) {
                        return item; // skip LOW confidence items
                    }
                    try {
                        ContextPackage ctx = ctxByFile.get(item.filePath());
                        String codeContext = ctx != null ? ctx.render() : item.filePath();
                        var suggestion = chatModelPort.generateSuggestion(
                                item.id(), item.description(), codeContext);
                        return item.withSuggestion(suggestion);
                    } catch (Exception e) {
                        log.warn("Suggestion generation failed for riskItem={}: {}",
                                item.id(), e.getMessage());
                        return item;
                    }
                })
                .collect(Collectors.toList());
    }

    private String buildAggregatedContext(List<ContextPackage> contextPackages) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Pull Request Analysis\n\n");
        for (ContextPackage ctx : contextPackages) {
            sb.append("### File: ").append(ctx.filePath()).append("\n");
            sb.append("Estimated tokens: ").append(ctx.estimatedTokens()).append("\n");
            // Include first hunk for summary context
            if (!ctx.hunks().isEmpty()) {
                sb.append(ctx.hunks().get(0).content(), 0,
                        Math.min(500, ctx.hunks().get(0).content().length()));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /** Result of a complete analysis run. */
    public record AnalysisResult(ChangeSummary summary, List<RiskItem> riskItems) {}
}
