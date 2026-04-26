package com.springAi.service;

import com.springAi.config.RabbitConfig;
import com.springAi.entity.DocumentFailureReason;
import com.springAi.entity.DocumentStatus;
import com.springAi.entity.vo.Result;
import com.springAi.mq.DocumentIngestMessage;
import com.springAi.repository.DocumentMetadataRepository;
import com.springAi.repository.DocumentMetadataRepository.DocumentRecord;
import com.springAi.repository.FileRepository;
import com.springAi.repository.VectorDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long PROCESS_TIMEOUT_SECONDS = 90L;

    private final FileRepository fileRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final RabbitTemplate rabbitTemplate;
    private final VectorDocumentRepository vectorDocumentRepository;

    public Result uploadPdf(String workspaceId, String uploadedBy, String originalFilename, String contentType, long fileSize, Resource resource) {
        String documentId = UUID.randomUUID().toString();
        boolean saved = fileRepository.save(workspaceId, documentId, resource);
        if (!saved) {
            documentMetadataRepository.save(new DocumentRecord(
                    documentId,
                    workspaceId,
                    originalFilename,
                    "enterprise-docs/" + workspaceId + "/" + documentId + "-" + originalFilename,
                    contentType,
                    fileSize,
                    DocumentStatus.FAILED,
                    DocumentFailureReason.SAVE_FAILED,
                    uploadedBy,
                    1,
                    true,
                    null,
                    null,
                    null
            ));
            return Result.fail("保存文件失败！");
        }

        documentMetadataRepository.save(new DocumentRecord(
                documentId,
                workspaceId,
                originalFilename,
                "enterprise-docs/" + workspaceId + "/" + documentId + "-" + originalFilename,
                contentType,
                fileSize,
                DocumentStatus.UPLOADED,
                null,
                uploadedBy,
                1,
                true,
                null,
                null,
                null
        ));

        enqueueIngest(workspaceId, documentId);
        return Result.ok(documentId);
    }

    public Result uploadNewVersion(String workspaceId, String parentDocumentId, String uploadedBy, String originalFilename, String contentType, long fileSize, Resource resource) {
        String documentId = UUID.randomUUID().toString();
        boolean saved = fileRepository.save(workspaceId, documentId, resource);
        if (!saved) {
            documentMetadataRepository.save(new DocumentRecord(documentId, workspaceId, originalFilename,
                    "enterprise-docs/" + workspaceId + "/" + documentId + "-" + originalFilename,
                    contentType, fileSize, DocumentStatus.FAILED, DocumentFailureReason.SAVE_FAILED,
                    uploadedBy, 1, true, parentDocumentId, null, null));
            return Result.fail("保存文件失败！");
        }
        documentMetadataRepository.markAllOlderVersionsNotLatest(workspaceId, parentDocumentId);
        documentMetadataRepository.save(new DocumentRecord(documentId, workspaceId, originalFilename,
                "enterprise-docs/" + workspaceId + "/" + documentId + "-" + originalFilename,
                contentType, fileSize, DocumentStatus.UPLOADED, null,
                uploadedBy, nextVersionNo(workspaceId, parentDocumentId), true, parentDocumentId, null, null));
        enqueueIngest(workspaceId, documentId);
        return Result.ok(documentId);
    }

    private int nextVersionNo(String workspaceId, String parentDocumentId) {
        List<DocumentRecord> versions = documentMetadataRepository.findVersionsByParent(workspaceId, parentDocumentId);
        return versions.isEmpty() ? 1 : versions.get(0).versionNo() + 1;
    }

    private void enqueueIngest(String workspaceId, String documentId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.DOC_INGEST_EXCHANGE,
                RabbitConfig.DOC_INGEST_ROUTING_KEY,
                new DocumentIngestMessage(workspaceId, documentId)
        );
    }

    @RabbitListener(queues = RabbitConfig.DOC_INGEST_QUEUE)
    public void consumeIngest(DocumentIngestMessage message, Message rawMessage) {
        String workspaceId = message.workspaceId();
        String documentId = message.documentId();
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        Integer retryCountHeader = rawMessage.getMessageProperties().getHeader("x-retry-count");
        int retryCount = retryCountHeader == null ? 0 : retryCountHeader;

        log.info("[doc-ingest] consume start workspaceId={}, documentId={}, retryCount={}, deliveryTag={}",
                workspaceId, documentId, retryCount, deliveryTag);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> processDocument(workspaceId, documentId));

        try {
            future.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("[doc-ingest] consume success workspaceId={}, documentId={}", workspaceId, documentId);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("[doc-ingest] timeout workspaceId={}, documentId={}, timeout={}s", workspaceId, documentId, PROCESS_TIMEOUT_SECONDS, e);
            documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.FAILED, DocumentFailureReason.INDEX_FAILED);
            requeueOrDeadLetter(message, retryCount, e);
        } catch (Exception e) {
            log.error("[doc-ingest] consume failed workspaceId={}, documentId={}, retryCount={}", workspaceId, documentId, retryCount, e);
            requeueOrDeadLetter(message, retryCount, e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void requeueOrDeadLetter(DocumentIngestMessage message, int retryCount, Exception e) {
        if (retryCount < MAX_RETRY_COUNT) {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.DOC_INGEST_EXCHANGE,
                    RabbitConfig.DOC_INGEST_ROUTING_KEY,
                    message,
                    msg -> {
                        msg.getMessageProperties().setHeader("x-retry-count", retryCount + 1);
                        return msg;
                    }
            );
            log.warn("[doc-ingest] requeue documentId={}, retry={} due to {}", message.documentId(), retryCount + 1, e.getClass().getSimpleName());
        } else {
            rabbitTemplate.convertAndSend(RabbitConfig.DOC_INGEST_DLX_EXCHANGE, RabbitConfig.DOC_INGEST_DLX_ROUTING_KEY, message);
            log.error("[doc-ingest] send to DLQ documentId={}, retryCount={} due to {}", message.documentId(), retryCount, e.getClass().getSimpleName());
        }
    }

    public void processDocument(String workspaceId, String documentId) {
        Resource resource = fileRepository.getFile(workspaceId, documentId);
        if (!resource.exists()) {
            log.error("[doc-ingest] file not found documentId={}, workspaceId={}", documentId, workspaceId);
            documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.FAILED, DocumentFailureReason.SAVE_FAILED);
            return;
        }

        try {
            if (!documentMetadataRepository.updateStatus(documentId, DocumentStatus.PROCESSING)) {
                log.warn("[doc-ingest] skip processing document {} because status transition is not allowed", documentId);
                return;
            }

            log.info("[doc-ingest] reading pdf documentId={}", documentId);
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .withPagesPerDocument(1)
                            .build()
            );

            List<Document> documents = reader.read();
            log.info("[doc-ingest] read done documentId={}, pages={}", documentId, documents.size());

            List<Document> chunks = tokenTextSplitter.apply(documents);
            log.info("[doc-ingest] split done documentId={}, chunks={}", documentId, chunks.size());

            chunks.forEach(doc -> {
                doc.getMetadata().put("workspaceId", workspaceId);
                doc.getMetadata().put("documentId", documentId);
            });

            // 幂等保护：重试/重复消费前先清理该文档既有向量片段
            vectorDocumentRepository.deleteByWorkspaceAndDocument(workspaceId, documentId);

            log.info("[doc-ingest] vector add start documentId={}, totalChunks={}", documentId, chunks.size());
            final int embeddingBatchSize = 10;
            for (int start = 0; start < chunks.size(); start += embeddingBatchSize) {
                int end = Math.min(start + embeddingBatchSize, chunks.size());
                List<Document> batch = chunks.subList(start, end);
                log.info("[doc-ingest] vector add batch documentId={}, range=[{}, {})", documentId, start, end);
                vectorStore.add(batch);
            }
            log.info("[doc-ingest] vector add done documentId={}", documentId);

            documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.INDEXED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("[doc-ingest] parse failed documentId={}, workspaceId={}", documentId, workspaceId, e);
            documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.FAILED, DocumentFailureReason.PARSE_FAILED);
            throw e;
        } catch (Exception e) {
            log.error("[doc-ingest] index failed documentId={}, workspaceId={}", documentId, workspaceId, e);
            documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.FAILED, DocumentFailureReason.INDEX_FAILED);
            throw e;
        }
    }

    public Result reindex(String workspaceId, String documentId, Resource resource) {
        try {
            enqueueIngest(workspaceId, documentId);
            return Result.ok("reindex started");
        } catch (Exception e) {
            return Result.fail("重建索引失败");
        }
    }
}
