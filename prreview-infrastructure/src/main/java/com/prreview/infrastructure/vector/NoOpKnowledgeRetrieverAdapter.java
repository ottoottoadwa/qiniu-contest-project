package com.prreview.infrastructure.vector;

import com.prreview.domain.port.out.KnowledgeRetrieverPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op implementation of KnowledgeRetrieverPort.
 * Returns empty results for MVP — RAG is a future extension.
 *
 * <p>Future: replace with PgVectorKnowledgeAdapter using Spring AI VectorStore
 * and pgvector extension for team knowledge base retrieval.
 */
@Slf4j
@Component
public class NoOpKnowledgeRetrieverAdapter implements KnowledgeRetrieverPort {

    @Override
    public List<String> retrieve(String query, int topK) {
        log.debug("Knowledge retrieval skipped (NoOp): query={}", query);
        return List.of();
    }
}
