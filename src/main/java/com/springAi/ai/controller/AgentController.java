package com.springAi.ai.controller;



import com.springAi.ai.config.AgentOrchestrator;
import com.springAi.ai.entity.ChatMessage;
import com.springAi.ai.repository.ChatHistoryRepository;
import com.springAi.ai.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;


@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class AgentController {

    private final ChatMessageRepository messageRepo;




    @GetMapping("/history/messages/{chatId}")
    public List<ChatMessage> getHistory(@PathVariable String chatId) {
        return messageRepo.getMessagesByChatId(chatId);
    }


    private final AgentOrchestrator agentOrchestrator;   // 替换 ReactAgent
    private final ChatClient chatClient;                 // 多模态仍用 ChatClient
    private final ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        chatHistoryRepository.save("chat", chatId);

        if (files == null || files.isEmpty()) {
            // 纯文本对话 -> 使用官方 AgentOrchestrator（同步，转 Flux）
            String answer = agentOrchestrator.chat(prompt,chatId);   // 注意：chatId 可用于扩展记忆，当前暂未使用
            return Flux.just(answer);
        } else {
            // 多模态 -> 使用 ChatClient
            return multiModalChat(prompt, chatId, files);
        }
    }

    private Flux<String> multiModalChat(String prompt, String chatId, List<MultipartFile> files) {
        List<Media> medias = files.stream()
                .map(file -> new Media(
                        MimeType.valueOf(Objects.requireNonNull(file.getContentType())),
                        file.getResource()
                ))
                .toList();

        // 视觉模型会自动使用 qwen-vl-max（需要在 application.yml 中配置 model）
        return chatClient.prompt()
                .user(u -> u.text(prompt).media(medias.toArray(Media[]::new)))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    private Flux<String> textChat(String prompt, String chatId) {
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();

    }






}
