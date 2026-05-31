package com.prreview.domain.port.out;

import java.util.List;

/**
 * Outbound port for retrieving relevant knowledge chunks (RAG).
 * MVP implementation returns empty list (NoOpKnowledgeRetrieverAdapter).
 * Future: pgvector-backed semantic search.
 */
public interface KnowledgeRetrieverPort {

    /**
     * Retrieves relevant knowledge chunks for the given query.
     *
     * @param query   semantic query derived from the code context
     * @param topK    maximum number of chunks to return
     * @return list of relevant text chunks (empty if RAG not enabled)
     */
    List<String> retrieve(String query, int topK);
}
