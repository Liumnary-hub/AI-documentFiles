package com.springAi.ai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatHistoryRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;  // 使用 final + 构造器注入

    // 保存对话 ID（左侧历史列表）
    public void save(String type, String chatId) {
        String sql = """
            INSERT IGNORE INTO chat_history (id, type, create_time)
            VALUES (?, ?, NOW())
        """;
        jdbcTemplate.update(sql, chatId, type);
    }

    // 获取所有对话 ID（左侧列表）
    public List<String> getChatIds(String type) {
        String sql = """
            SELECT id FROM chat_history
            WHERE type = ?
            ORDER BY create_time DESC
        """;
        return jdbcTemplate.queryForList(sql, String.class, type);
    }
}