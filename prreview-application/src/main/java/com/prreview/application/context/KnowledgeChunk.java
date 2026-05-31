package com.prreview.application.context;

/**
 * A chunk of knowledge retrieved from the team knowledge base (L4 context layer).
 * MVP: always empty (NoOp retriever). Future: pgvector RAG.
 */
public record KnowledgeChunk(
        String content,
        String sourceType,
        double relevanceScore) {}
