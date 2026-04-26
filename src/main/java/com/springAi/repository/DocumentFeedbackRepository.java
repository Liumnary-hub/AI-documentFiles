package com.springAi.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DocumentFeedbackRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public void save(String workspaceId, String conversationId, String documentId,
                     String prompt, String answer, int rating, String comment) {
        String sql = """
            INSERT INTO document_feedback
            (workspace_id, conversation_id, document_id, prompt, answer, rating, comment, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
        """;
        jdbcTemplate.update(sql, workspaceId, conversationId, documentId, prompt, answer, rating, comment);
    }
}
