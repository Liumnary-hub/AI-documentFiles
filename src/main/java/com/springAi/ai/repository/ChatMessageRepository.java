package com.springAi.ai.repository;

import com.springAi.ai.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    // 保存一条消息
    public void saveMessage(String chatId, String role, String content) {
        String sql = """
            INSERT INTO chat_message (chat_id, role, content, create_time)
            VALUES (?, ?, ?, NOW())
        """;
        jdbcTemplate.update(sql, chatId, role, content);
    }

    // 查询会话的所有消息（按时间正序）
    public List<ChatMessage> getMessagesByChatId(String chatId) {
        String sql = """
            SELECT id, chat_id, role, content, create_time
            FROM chat_message
            WHERE chat_id = ?
            ORDER BY create_time ASC
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ChatMessage(
                rs.getLong("id"),
                rs.getString("chat_id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getTimestamp("create_time").toLocalDateTime()
        ), chatId);
    }

    // 可选：删除会话的所有消息（当删除会话时调用）
    public void deleteMessagesByChatId(String chatId) {
        jdbcTemplate.update("DELETE FROM chat_message WHERE chat_id = ?", chatId);
    }
}