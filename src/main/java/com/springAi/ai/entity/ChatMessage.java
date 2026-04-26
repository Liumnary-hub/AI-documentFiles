package com.springAi.ai.entity;

import java.time.LocalDateTime;

public class ChatMessage {
    private Long id;
    private String chatId;
    private String role;   // user, assistant
    private String content;
    private LocalDateTime createTime;

    // 全参构造器（用于查询）
    public ChatMessage(Long id, String chatId, String role, String content, LocalDateTime createTime) {
        this.id = id;
        this.chatId = chatId;
        this.role = role;
        this.content = content;
        this.createTime = createTime;
    }

    // Getters and Setters（Lombok 可简化，这里手动生成）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}