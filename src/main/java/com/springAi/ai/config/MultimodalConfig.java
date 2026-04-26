package com.springAi.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultimodalConfig {

    @Bean
    @Qualifier("multimodalChatClient")
    public ChatClient multimodalChatClient(OpenAiApi openAiApi) {
        // 使用视觉模型 qwen-vl-max
        var chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-vl-max")   // 视觉语言模型
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }
}