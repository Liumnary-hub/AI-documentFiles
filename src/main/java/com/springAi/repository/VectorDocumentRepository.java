package com.springAi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@Slf4j
@Repository
public class VectorDocumentRepository {

    private final VectorStore vectorStore;

    public VectorDocumentRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void deleteByWorkspaceAndDocument(String workspaceId, String documentId) {
        try {
            Filter.Expression expression = new FilterExpressionBuilder()
                    .and(
                            new FilterExpressionBuilder().eq("workspaceId", workspaceId),
                            new FilterExpressionBuilder().eq("documentId", documentId)
                    )
                    .build();
            vectorStore.delete(expression);
        } catch (Exception e) {
            log.warn("Failed to delete vector docs for workspace={}, documentId={}", workspaceId, documentId, e);
        }
    }
}
