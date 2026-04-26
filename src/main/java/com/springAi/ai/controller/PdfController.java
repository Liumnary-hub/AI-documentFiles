package com.springAi.ai.controller;

import com.springAi.ai.entity.vo.Result;
import com.springAi.ai.repository.ChatHistoryRepository;
import com.springAi.ai.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    private final FileRepository fileRepository;
    private final VectorStore vectorStore;
    private final ChatClient pdfChatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId) {
        Resource file = fileRepository.getFile(chatId);
        if (!file.exists()) {
            throw new RuntimeException("会话文件不存在！");
        }
        chatHistoryRepository.save("pdf", chatId);

        // 动态构造过滤表达式字符串：只检索 chatId == 当前会话ID 的文档
        String filterExpression = "chatId == '" + chatId + "'";

        return pdfChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression)
                )
                .stream()
                .content();
    }
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        try {
            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                return Result.fail("只能上传PDF文件！");
            }
            boolean success = fileRepository.save(chatId, file.getResource());
            if (!success) {
                return Result.fail("保存文件失败！");
            }

            // 只保留这行！！！
            this.writeToVectorStore(file.getResource(), chatId);

            return Result.ok();
        } catch (Exception e) {
            log.error("Failed to upload PDF.", e);
            return Result.fail("上传文件失败！");
        }
    }

    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId) throws IOException {
        Resource resource = fileRepository.getFile(chatId);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ==========================================
    // 增加 chatId 参数
    // ==========================================
    private void writeToVectorStore(Resource resource, String chatId) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1)
                        .build()
        );

        List<Document> documents = reader.read();

        // 只绑定 metadata，不碰 ID！
        documents.forEach(doc -> {
            doc.getMetadata().put("chatId", chatId);
        });

        vectorStore.add(documents);
    }
}