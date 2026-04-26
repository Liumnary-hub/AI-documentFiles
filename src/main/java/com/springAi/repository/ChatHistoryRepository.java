package com.springAi.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatHistoryRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public void save(String type, String conversationId, String workspaceId) {
        String sql = """
            INSERT INTO chat_history (id, type, workspace_id, create_time)
            VALUES (?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                type = VALUES(type),
                workspace_id = VALUES(workspace_id),
                create_time = NOW()
        """;
        jdbcTemplate.update(sql, conversationId, type, workspaceId);
    }

    public void save(String type, String conversationId) {
        save(type, conversationId, "default");
    }

    public List<String> getChatIds(String type, String workspaceId) {
        String sql = """
            SELECT id FROM chat_history
            WHERE type = ?
              AND workspace_id = ?
            ORDER BY create_time DESC
        """;
        return jdbcTemplate.queryForList(sql, String.class, type, workspaceId);
    }
}