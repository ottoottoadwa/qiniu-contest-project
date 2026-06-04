package com.prreview.application.review;

import com.prreview.domain.model.review.Review;
import com.prreview.domain.model.risk.RiskItem;
import com.prreview.domain.model.risk.Severity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats review results as Markdown for GitHub comments.
 */
@Component
public class ResultFormatter {

    /**
     * Formats a completed review as a GitHub comment.
     *
     * @param review completed review
     * @return Markdown-formatted comment
     */
    public String formatAsComment(Review review) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("## 🤖 代码审查结果\n\n");

        // Summary
        if (review.getSummary() != null) {
            sb.append("### 📋 概要\n\n");
            sb.append("**").append(review.getSummary().headline()).append("**\n\n");
            if (review.getSummary().inferredPurpose() != null && !review.getSummary().inferredPurpose().isEmpty()) {
                sb.append(review.getSummary().inferredPurpose()).append("\n\n");
            }

            if (!review.getSummary().affectedModules().isEmpty()) {
                sb.append("**影响模块:** ");
                sb.append(String.join(", ", review.getSummary().affectedModules()));
                sb.append("\n\n");
            }
        }

        // Risk items grouped by severity
        List<RiskItem> riskItems = review.getRiskItems();
        if (riskItems.isEmpty()) {
            sb.append("### ✅ 未发现问题\n\n");
            sb.append("代码质量良好！本次 PR 未检测到明显风险。\n\n");
        } else {
            Map<Severity, List<RiskItem>> grouped = riskItems.stream()
                    .collect(Collectors.groupingBy(RiskItem::severity));

            // Critical issues
            if (grouped.containsKey(Severity.CRITICAL)) {
                sb.append("### 🚨 严重问题 (").append(grouped.get(Severity.CRITICAL).size()).append(")\n\n");
                formatRiskItems(sb, grouped.get(Severity.CRITICAL));
            }

            // High severity
            if (grouped.containsKey(Severity.HIGH)) {
                sb.append("### ⚠️ 高优先级 (").append(grouped.get(Severity.HIGH).size()).append(")\n\n");
                formatRiskItems(sb, grouped.get(Severity.HIGH));
            }

            // Medium severity
            if (grouped.containsKey(Severity.MEDIUM)) {
                sb.append("### ⚡ 中优先级 (").append(grouped.get(Severity.MEDIUM).size()).append(")\n\n");
                formatRiskItems(sb, grouped.get(Severity.MEDIUM));
            }

            // Low severity
            if (grouped.containsKey(Severity.LOW)) {
                sb.append("### 💡 低优先级 (").append(grouped.get(Severity.LOW).size()).append(")\n\n");
                formatRiskItems(sb, grouped.get(Severity.LOW));
            }
        }

        // Footer
        sb.append("---\n");
        sb.append("*由 PRReview AI 提供支持* | ");
        sb.append("审查 ID: `").append(review.getId()).append("`\n");

        return sb.toString();
    }

    private void formatRiskItems(StringBuilder sb, List<RiskItem> items) {
        for (RiskItem item : items) {
            sb.append("<details>\n");
            sb.append("<summary>");
            sb.append("<strong>").append(item.category()).append("</strong> - ");
            sb.append(item.description().length() > 80
                    ? item.description().substring(0, 80) + "..."
                    : item.description());
            sb.append(" <code>").append(item.filePath());
            if (item.startLine() > 0) {
                sb.append(":").append(item.startLine());
            }
            sb.append("</code>");
            sb.append("</summary>\n\n");

            // Description
            sb.append(item.description()).append("\n\n");

            // Suggestion if available
            if (item.suggestion() != null) {
                sb.append("**💡 建议:**\n\n");
                sb.append(item.suggestion().explanation()).append("\n\n");

                if (item.suggestion().recommendation() != null && !item.suggestion().recommendation().isEmpty()) {
                    sb.append("**推荐做法:**\n\n");
                    sb.append(item.suggestion().recommendation()).append("\n\n");
                }

                // Code patch if available
                if (item.suggestion().suggestedPatch() != null) {
                    sb.append("**建议修复:**\n\n");
                    sb.append("```diff\n");
                    sb.append(item.suggestion().suggestedPatch());
                    sb.append("\n```\n\n");
                }
            }

            sb.append("</details>\n\n");
        }
    }

    /**
     * Formats a progress update message.
     *
     * @param filesAnalyzed number of files analyzed
     * @param totalFiles    total number of files
     * @return progress message
     */
    public String formatProgress(int filesAnalyzed, int totalFiles) {
        int percentage = (int) ((double) filesAnalyzed / totalFiles * 100);
        return String.format("🔍 分析中... %d/%d 个文件 (%d%%)", filesAnalyzed, totalFiles, percentage);
    }

    /**
     * Formats an error message.
     *
     * @param error error message
     * @return formatted error comment
     */
    public String formatError(String error) {
        return "## ❌ 审查失败\n\n" +
                "审查过程中发生错误:\n\n" +
                "```\n" + error + "\n```\n\n" +
                "请检查日志或稍后重试。";
    }
}
