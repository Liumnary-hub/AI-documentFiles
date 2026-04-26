package com.springAi.ai.controller;

import com.springAi.ai.repository.ChatHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;  // 新增导入
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class CustomerServiceController {

    private final ChatClient rebornGameChatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    public CustomerServiceController(
            @Qualifier("rebornGameChatClient") ChatClient rebornGameChatClient,
            ChatHistoryRepository chatHistoryRepository) {
        this.rebornGameChatClient = rebornGameChatClient;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    @RequestMapping(value = "/service", produces = "text/html;charset=utf-8")
    public Flux<String> game(String prompt, String chatId) {
        if (!StringUtils.hasText(prompt)) {
            return Flux.just("【系统提示】请输入你的行动或选择～");
        }
        chatHistoryRepository.save("reborn", chatId);
        return rebornGameChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))  // 修改这一行
                .stream()
                .content();
    }
}