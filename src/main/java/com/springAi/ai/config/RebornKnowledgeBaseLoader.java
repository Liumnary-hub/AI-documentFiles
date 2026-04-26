package com.springAi.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class  RebornKnowledgeBaseLoader implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    @Value("classpath:rebirth_novels/*.txt")
    private Resource[] novelResources;

    public RebornKnowledgeBaseLoader(@Qualifier("pgVectorStore")VectorStore vectorStore, TokenTextSplitter textSplitter) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("开始加载重生小说知识库...");
        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : novelResources) {
            try {
                // 1. 读取小说文件内容
                String content = Files.readString(Path.of(resource.getURI()));
                // 创建Document对象，并附加元数据（如文件名、章节标题等，便于追踪）
                Document doc = new Document(content,
                        java.util.Map.of("source", resource.getFilename()));
                allDocuments.add(doc);
                System.out.println("已加载文件: " + resource.getFilename());
            } catch (IOException e) {
                System.err.println("读取文件失败: " + resource.getFilename() + ", 错误: " + e.getMessage());
            }
        }

        if (!allDocuments.isEmpty()) {
            // 2. 切分文档为更小的块
            List<Document> chunks = textSplitter.apply(allDocuments);
            // 3. 向量化并存储到数据库
            vectorStore.add(chunks);
            System.out.println("知识库加载完成！共处理 " + allDocuments.size() + " 个文件，生成 " + chunks.size() + " 个知识片段。");
        } else {
            System.out.println("未找到任何小说文件，请检查 resources/rebirth_novels/ 目录。");
        }
    }


}