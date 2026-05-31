package com.prreview.infrastructure.rules;

import com.prreview.domain.model.risk.AiRiskFinding;
import com.prreview.domain.model.risk.RiskCategory;
import com.prreview.domain.model.risk.Severity;
import com.prreview.domain.port.out.RuleEnginePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static rule engine adapter implementing common code smell and security patterns.
 * Uses regex-based rules for deterministic, zero-hallucination detection.
 * Covers OWASP Top 10 patterns and common Java anti-patterns.
 */
@Slf4j
@Component
public class StaticRuleEngineAdapter implements RuleEnginePort {

    private static final List<Rule> RULES = List.of(
            // Security rules
            new Rule("SEC-001", RiskCategory.SECURITY, Severity.CRITICAL,
                    Pattern.compile("(?i)(password|secret|api_?key|token)\\s*=\\s*[\"'][^\"']{4,}[\"']"),
                    "Hardcoded credential detected",
                    "Hardcoded secrets in source code are a critical security risk. Use environment variables or a secrets manager."),
            new Rule("SEC-002", RiskCategory.SECURITY, Severity.HIGH,
                    Pattern.compile("(?i)Statement.*execute.*\\+|createQuery.*\\+.*\\+"),
                    "Potential SQL injection via string concatenation",
                    "String concatenation in SQL queries enables SQL injection. Use parameterized queries or Spring Data methods."),
            new Rule("SEC-003", RiskCategory.SECURITY, Severity.HIGH,
                    Pattern.compile("(?i)log\\.(info|debug|warn|error).*password|log\\.(info|debug|warn|error).*token"),
                    "Sensitive data may be logged",
                    "Logging passwords or tokens exposes sensitive data in log aggregators. Mask or omit sensitive fields."),
            new Rule("SEC-004", RiskCategory.SECURITY, Severity.MEDIUM,
                    Pattern.compile("new\\s+Random\\(\\)"),
                    "java.util.Random used for security-sensitive operation",
                    "java.util.Random is not cryptographically secure. Use java.security.SecureRandom for security-sensitive operations."),

            // Correctness rules
            new Rule("COR-001", RiskCategory.CORRECTNESS, Severity.HIGH,
                    Pattern.compile("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)\\s*\\{\\s*\\}"),
                    "Empty catch block swallows exception",
                    "Empty catch blocks hide errors silently. At minimum, log the exception or rethrow as a domain exception."),
            new Rule("COR-002", RiskCategory.CORRECTNESS, Severity.MEDIUM,
                    Pattern.compile("\\.equals\\(null\\)"),
                    "Calling .equals(null) always returns false",
                    "Use == null for null checks. Calling .equals(null) is always false and indicates a logic error."),
            new Rule("COR-003", RiskCategory.CORRECTNESS, Severity.MEDIUM,
                    Pattern.compile("@Transactional.*\\n.*private\\s"),
                    "@Transactional on private method — proxy bypass",
                    "@Transactional on private methods is ignored by Spring's proxy-based AOP. Move to a public method."),

            // Performance rules
            new Rule("PERF-001", RiskCategory.PERFORMANCE, Severity.MEDIUM,
                    Pattern.compile("for\\s*\\(.*\\)\\s*\\{[^}]*\\.findById\\(|for\\s*\\(.*\\)\\s*\\{[^}]*repository\\.find"),
                    "Potential N+1 query in loop",
                    "Repository calls inside loops cause N+1 queries. Use batch loading, @EntityGraph, or JOIN FETCH."),
            new Rule("PERF-002", RiskCategory.PERFORMANCE, Severity.LOW,
                    Pattern.compile("new\\s+ArrayList\\(\\)|new\\s+HashMap\\(\\)"),
                    "Collection created without initial capacity",
                    "Specify initial capacity when the size is known to avoid repeated resizing."),

            // Maintainability rules
            new Rule("MAINT-001", RiskCategory.MAINTAINABILITY, Severity.LOW,
                    Pattern.compile("@Autowired\\s+private"),
                    "Field injection via @Autowired",
                    "Field injection hides dependencies and prevents testing via constructor. Use constructor injection instead.")
    );

    @Override
    public List<AiRiskFinding> scan(String filePath, String codeSnippet,
                                     Set<RiskCategory> categories) {
        if (codeSnippet == null || codeSnippet.isBlank()) {
            return List.of();
        }

        List<AiRiskFinding> findings = new ArrayList<>();
        String[] lines = codeSnippet.split("\n");

        for (Rule rule : RULES) {
            if (!categories.isEmpty() && !categories.contains(rule.category())) {
                continue;
            }

            for (int i = 0; i < lines.length; i++) {
                if (rule.pattern().matcher(lines[i]).find()) {
                    findings.add(new AiRiskFinding(
                            filePath,
                            i + 1,
                            i + 1,
                            rule.category(),
                            rule.severity(),
                            0.9, // rules have high confidence
                            rule.description(),
                            rule.rationale()));
                }
            }
        }

        log.debug("Rule scan for {}: {} findings", filePath, findings.size());
        return findings;
    }

    /** A static rule definition. */
    private record Rule(
            String id,
            RiskCategory category,
            Severity severity,
            Pattern pattern,
            String description,
            String rationale) {}
}
