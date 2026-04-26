package com.springAi.ai.config;

import com.springAi.ai.constants.SystemConstants;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class CommonConfiguration {

//    @Bean
//    public OpenAiApi openAiApi() {
//        return OpenAiApi.builder()
//                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
//                .apiKey("sk-35c1c4253bde4f3fb741025b3621584e")
//                .build();
//    }
//
//    @Bean
//    @Primary
//    public ChatModel chatModel(OpenAiApi openAiApi) {
//        // 使用 defaultOptions 方法来配置模型
//        return OpenAiChatModel.builder()
//                .openAiApi(openAiApi)
//                .defaultOptions(OpenAiChatOptions.builder()
//                        .model("qwen-vl-max")
//                        .build())
//                .build();
//    }
//
//
//
//
//    @Bean
//    @Primary
//    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
//        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
//                .model("text-embedding-v3")
//                .build();
//
//        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
//    }




    private final JdbcTemplate mysqlJdbcTemplate;
    private final JdbcTemplate pgJdbcTemplate;

    public CommonConfiguration(@Qualifier("mysqlJdbcTemplate") JdbcTemplate mysqlJdbcTemplate,
                               @Qualifier("pgJdbcTemplate") JdbcTemplate pgJdbcTemplate) {
        this.mysqlJdbcTemplate = mysqlJdbcTemplate;
        this.pgJdbcTemplate = pgJdbcTemplate;
    }




    @Bean
    public ChatMemory chatMemory(@Qualifier("mysqlDataSource") DataSource dataSource) {
        MysqlChatMemory memory = new MysqlChatMemory(dataSource);
        memory.setDefaultMaxRecords(10);   // 设置最大记录数为 10
        return memory;
    }

    @Bean
    public VectorStore pgVectorStore(EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(pgJdbcTemplate, embeddingModel)
                .dimensions(1024)
                .initializeSchema(true)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .build();
    }

    // ---------- 公共常量 ----------
    private static final String REBORN_SYSTEM_PROMPT = """
            你是一个重生爽文互动游戏的AI导演，名字叫“重生小帅”。
            你要用沉浸、紧凑且充满爽点的语气与玩家互动，负责构建真实的重生世界、推动剧情、给出选项。
            【核心规则】
            1. 每次回复必须包含【剧情叙述】、【系统提示】（可选）、【请选择】三个部分。
            2. 剧情必须符合“重生爽文”逻辑：主角利用前世记忆或金手指，逐步打脸、逆袭。
            3. 每次必须提供2-3个清晰、有差异性的选项。
            4. 严禁AI替玩家做决定，也严禁在叙述中直接写出玩家的内心独白。
            """;

    // ---------- ChatClient 实例 ----------

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(), memoryAdvisor)
                .build();
    }



    @Bean
    public ChatClient rebornGameChatClient(
            ChatModel chatModel,
            ChatMemory chatMemory,
            VectorStore pgVectorStore) {

        var memoryAdvisor =  MessageChatMemoryAdvisor.builder(chatMemory).build();

        var searchRequest = SearchRequest.builder()
                .topK(4)
                .similarityThreshold(0.7)
                .build();
        var ragAdvisor = QuestionAnswerAdvisor.builder(pgVectorStore)
                .searchRequest(searchRequest)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(REBORN_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        ragAdvisor,
                        memoryAdvisor
                )
                .build();
    }

    @Bean
    public ChatClient gameChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        var memoryAdvisor =  MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        memoryAdvisor
                )
                .build();
    }
    @Bean
    public ChatClient pdfChatClient(
            ChatModel chatModel,
            ChatMemory chatMemory,
            VectorStore vectorStore) {

        var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 注意：这里不写死 filterExpression，改为在调用时动态传入
        var ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(5).build())
                // 不设置 .filterExpression()，留给运行时
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem("你根据PDF内容回答问题，不知道就说不知道。")
                .defaultAdvisors(memoryAdvisor, ragAdvisor)
                .build();
    }
}