package com.springAi.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)        // 对应原来的 defaultChunkSize
                .withMinChunkSizeChars(50) // 对应原来的 minChunkSizeChars
                .withMinChunkLengthToEmbed(5) // 对应原来的 minChunkLengthToEmbed
                .withMaxNumChunks(10000)   // 对应原来的 maxNumChunks
                .withKeepSeparator(true)   // 对应原来的 keepSeparator
                .build();
    }
}