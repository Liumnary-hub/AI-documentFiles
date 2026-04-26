package com.springAi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springAi.entity.vo.SourceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final JdbcTemplate pgJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HybridSearchService(VectorStore vectorStore, @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate) {
        this.vectorStore = vectorStore;
        this.pgJdbcTemplate = pgJdbcTemplate;
    }

    public List<SourceVO> vectorOnlySearch(String workspaceId, String query, int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 20));
        List<Document> semanticDocs = semanticSearch(workspaceId, query, safeTopK);
        return semanticDocs.stream()
                .map(doc -> new SourceVO(doc.getText(), doc.getMetadata()))
                .limit(safeTopK)
                .toList();
    }

    public List<SourceVO> vectorSearch(String workspaceId, String query, int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 20));
        return semanticSearch(workspaceId, query, safeTopK).stream()
                .map(doc -> new SourceVO(doc.getText(), doc.getMetadata()))
                .limit(safeTopK)
                .toList();
    }

    public List<SourceVO> hybridSearch(String workspaceId, String query, int topK) {
        int safeTopK = Math.max(1, Math.min(topK, 20));

        List<Document> semanticDocs = semanticSearch(workspaceId, query, safeTopK);
        List<SourceVO> keywordDocs = keywordSearch(workspaceId, query, safeTopK);

        // Reciprocal Rank Fusion (RRF)
        Map<String, RankedSource> ranked = new LinkedHashMap<>();
        final double k = 60.0;

        for (int i = 0; i < semanticDocs.size(); i++) {
            Document doc = semanticDocs.get(i);
            SourceVO source = new SourceVO(doc.getText(), doc.getMetadata());
            String key = keyOf(source);
            ranked.computeIfAbsent(key, x -> new RankedSource(source)).score += 1.0 / (k + i + 1);
        }

        for (int i = 0; i < keywordDocs.size(); i++) {
            SourceVO source = keywordDocs.get(i);
            String key = keyOf(source);
            ranked.computeIfAbsent(key, x -> new RankedSource(source)).score += 1.0 / (k + i + 1);
        }

        return ranked.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(safeTopK)
                .map(rs -> rs.source)
                .toList();
    }

    private List<Document> semanticSearch(String workspaceId, String query, int topK) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.0)
                    .filterExpression("workspaceId == '" + workspaceId + "'")
                    .build();
            List<Document> docs = vectorStore.similaritySearch(request);
            return docs == null ? List.of() : docs;
        } catch (Exception e) {
            log.warn("[hybrid-search] semantic search failed, workspaceId={}", workspaceId, e);
            return List.of();
        }
    }

    private List<SourceVO> keywordSearch(String workspaceId, String query, int topK) {
        try {
            // 使用 PostgreSQL 语法；metadata 列是 JSON 字符串，先转 jsonb 再取字段
            String sql = """
                    SELECT content, metadata
                    FROM vector_store
                    WHERE CAST(metadata AS jsonb) ->> 'workspaceId' = ?
                      AND content ILIKE ?
                    ORDER BY id DESC
                    LIMIT ?
                    """;
            String like = "%" + query.trim() + "%";
            return pgJdbcTemplate.query(sql, (rs, rowNum) -> {
                String content = rs.getString("content");
                String metadataRaw = rs.getString("metadata");
                Map<String, Object> metadata = parseMetadata(metadataRaw);
                return new SourceVO(content, metadata);
            }, workspaceId, like, topK);
        } catch (Exception e) {
            log.warn("[hybrid-search] keyword search failed, workspaceId={}", workspaceId, e);
            return List.of();
        }
    }

    private Map<String, Object> parseMetadata(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("raw", raw);
        }
    }

    private String keyOf(SourceVO source) {
        String docId = source.getDocumentId() == null ? "-" : source.getDocumentId();
        String page = source.getPage() == null ? "-" : source.getPage().toString();
        String content = source.getContent() == null ? "" : source.getContent();
        String prefix = content.length() > 64 ? content.substring(0, 64) : content;
        return docId + "|" + page + "|" + prefix;
    }

    private static class RankedSource {
        SourceVO source;
        double score;

        RankedSource(SourceVO source) {
            this.source = source;
            this.score = 0;
        }
    }
}
