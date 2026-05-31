package com.prreview.application.context;

import com.prreview.domain.model.pr.FileChange;
import com.prreview.domain.model.pr.PullRequest;
import com.prreview.domain.port.out.KnowledgeRetrieverPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Assembles a ContextPackage for each changed file.
 * Implements the layered context strategy from docs/04 §3.
 *
 * <p>Layer priority (L0 always included, L4 optional):
 * <ol>
 *   <li>L0: diff hunks (mandatory)</li>
 *   <li>L1: enclosing function/class definitions (mandatory)</li>
 *   <li>L2: related callers/callees (on-demand for public API changes)</li>
 *   <li>L3: PR semantics — title, description, intent</li>
 *   <li>L4: team knowledge base RAG (optional, MVP = empty)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextAssembler {

    private final TokenBudgetPlanner tokenBudgetPlanner;
    private final KnowledgeRetrieverPort knowledgeRetriever;

    /**
     * Assembles context packages for all changed files in a PR.
     *
     * @param pr          the pull request metadata
     * @param fileChanges list of changed files with diff hunks
     * @return list of context packages, one per file
     */
    public List<ContextPackage> assemble(PullRequest pr, List<FileChange> fileChanges) {
        PrSemantics prSemantics = buildPrSemantics(pr);

        return fileChanges.stream()
                .map(fc -> assembleForFile(fc, prSemantics))
                .toList();
    }

    private ContextPackage assembleForFile(FileChange fileChange, PrSemantics prSemantics) {
        log.debug("Assembling context for file: {}", fileChange.path());

        // L0: diff hunks (always included)
        // L1: enclosing definitions — simplified for MVP (full content as placeholder)
        String enclosingDefs = buildEnclosingDefinitions(fileChange);

        // L2: related snippets — skipped in MVP (would require symbol indexing)
        List<RelatedSnippet> related = List.of();

        // L4: knowledge retrieval — MVP returns empty
        List<KnowledgeChunk> knowledge = retrieveKnowledge(fileChange.path());

        // Estimate token count and apply budget planning
        int estimatedTokens = tokenBudgetPlanner.estimate(
                fileChange.hunks(), enclosingDefs, related, knowledge);

        return new ContextPackage(
                fileChange.path(),
                fileChange.hunks(),
                enclosingDefs,
                related,
                prSemantics,
                knowledge,
                estimatedTokens);
    }

    private PrSemantics buildPrSemantics(PullRequest pr) {
        return new PrSemantics(
                pr.title() != null ? pr.title() : "",
                pr.description() != null ? pr.description() : "",
                pr.author() != null ? pr.author() : "",
                pr.baseSha() != null ? pr.baseSha() : "",
                pr.headSha() != null ? pr.headSha() : "");
    }

    private String buildEnclosingDefinitions(FileChange fileChange) {
        // MVP: return a summary of the diff hunks as L1 context.
        // Production: would use tree-sitter or language-specific parser to extract
        // the full enclosing function/class definition for each changed line.
        if (fileChange.hunks().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("// File: ").append(fileChange.path()).append("\n");
        sb.append("// Change type: ").append(fileChange.changeType()).append("\n");
        sb.append("// Lines added: ").append(fileChange.additions())
          .append(", deleted: ").append(fileChange.deletions()).append("\n");
        return sb.toString();
    }

    private List<KnowledgeChunk> retrieveKnowledge(String filePath) {
        try {
            List<String> chunks = knowledgeRetriever.retrieve(filePath, 3);
            return chunks.stream()
                    .map(c -> new KnowledgeChunk(c, "KNOWLEDGE_BASE", 1.0))
                    .toList();
        } catch (Exception e) {
            log.warn("Knowledge retrieval failed for {}: {}", filePath, e.getMessage());
            return List.of();
        }
    }
}
