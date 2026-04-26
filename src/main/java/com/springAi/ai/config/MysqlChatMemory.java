package com.springAi.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysqlChatMemory implements ChatMemory {

    private final JdbcTemplate jdbcTemplate;
    private int defaultMaxRecords = 100; // 默认最大记录数

    public MysqlChatMemory(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void setDefaultMaxRecords(int defaultMaxRecords) {
        this.defaultMaxRecords = defaultMaxRecords;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            String sql = "INSERT INTO spring_ai_chat_memory (conversation_id, message_type, message_text) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, conversationId, message.getMessageType().name(), message.getText());
        }
    }


    public List<Message> get(String conversationId, int maxRecords) {
        int limit = maxRecords > 0 ? maxRecords : this.defaultMaxRecords;
        // 查询最新的 limit 条消息（按 id 倒序），然后反转顺序
        String sql = "SELECT * FROM spring_ai_chat_memory WHERE conversation_id = ? ORDER BY id DESC LIMIT ?";
        List<Message> messages = jdbcTemplate.query(sql, new Object[]{conversationId, limit}, (rs, rowNum) -> {
            String type = rs.getString("message_type");
            String text = rs.getString("message_text");
            if (type.equals(MessageType.USER.name())) {
                return new UserMessage(text);
            } else if (type.equals(MessageType.ASSISTANT.name())) {
                return new AssistantMessage(text);
            }
            return null;
        });
        // 反转成时间正序
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, this.defaultMaxRecords);
    }

    @Override
    public void clear(String conversationId) {
        String sql = "DELETE FROM spring_ai_chat_memory WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }
}