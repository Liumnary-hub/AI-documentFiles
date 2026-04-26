package com.springAi.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class PgVectorConfig {


    @Bean
    public PgVectorStore vectorStore(
            @Qualifier("pgDataSource") DataSource pgDataSource,
            EmbeddingModel embeddingModel
    ) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(pgDataSource);

        // 你的版本必须传2个参数
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1024)
                .initializeSchema(true)
                .build();
    }
}