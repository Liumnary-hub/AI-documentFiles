package com.springAi.controller;

import com.springAi.entity.vo.MessageVO;
import com.springAi.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMemory chatMemory;

    @GetMapping("/{type}/{workspaceId}")
    public List<String> getChatIds(@PathVariable("type") String type,
                                   @PathVariable("workspaceId") String workspaceId) {
        return chatHistoryRepository.getChatIds(type, workspaceId);
    }

    @GetMapping("/{type}/{workspaceId}/{conversationId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type,
                                          @PathVariable("workspaceId") String workspaceId,
                                          @PathVariable("conversationId") String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).toList();
    }
}
