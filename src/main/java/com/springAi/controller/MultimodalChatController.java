package com.springAi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/multimodal")
@RequiredArgsConstructor
public class MultimodalChatController {

    @Qualifier("multimodalChatClient")
    private final ChatClient multimodalChatClient;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            // 纯文本对话
            return multimodalChatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content();
        } else {
            // 多模态对话（图片+文本）
            List<Media> medias = files.stream()
                    .map(file -> new Media(
                            MimeType.valueOf(Objects.requireNonNull(file.getContentType())),
                            file.getResource()
                    ))
                    .toList();
            return multimodalChatClient.prompt()
                    .user(u -> u.text(prompt).media(medias.toArray(Media[]::new)))
                    .stream()
                    .content();
        }
    }
}