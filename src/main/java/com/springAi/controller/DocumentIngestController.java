package com.springAi.controller;

import com.springAi.entity.vo.Result;
import com.springAi.service.DocumentIngestDlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/documents/ingest")
public class DocumentIngestController {

    private final DocumentIngestDlqService dlqService;

    @GetMapping("/dlq")
    public Map<String, Object> dlqSummary() {
        return dlqService.summary();
    }

    @PostMapping("/dlq/retry-all")
    public Result retryAll() {
        int retried = dlqService.retryAll();
        return Result.ok("已重试 " + retried + " 条死信消息");
    }

    @PostMapping("/dlq/retry/{documentId}")
    public Result retryByDocumentId(@PathVariable String documentId) {
        int retried = dlqService.retryByDocumentId(documentId);
        return Result.ok("已重试 documentId=" + documentId + " 的 " + retried + " 条死信消息");
    }
}
