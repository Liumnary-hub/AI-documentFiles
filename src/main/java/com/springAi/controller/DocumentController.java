package com.springAi.controller;

import com.springAi.entity.DocumentStatus;
import com.springAi.entity.vo.DocumentStatusVO;
import com.springAi.entity.vo.DocumentVersionVO;
import com.springAi.entity.vo.PageResult;
import com.springAi.entity.vo.Result;
import com.springAi.repository.AuditLogRepository;
import com.springAi.repository.DocumentMetadataRepository;
import com.springAi.repository.DocumentMetadataRepository.DocumentRecord;
import com.springAi.repository.FileRepository;
import com.springAi.repository.VectorDocumentRepository;
import com.springAi.repository.WorkspaceMemberRepository;
import com.springAi.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/documents")
public class DocumentController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentIngestionService documentIngestionService;
    private final AuditLogRepository auditLogRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final VectorDocumentRepository vectorDocumentRepository;
    private final FileRepository fileRepository;

    @GetMapping("/{workspaceId}")
    public PageResult<DocumentRecord> listDocuments(@PathVariable String workspaceId,
                                                     @RequestParam String userId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return new PageResult<>(List.of(), 0, safePage, safeSize, 0);
        }
        long total = documentMetadataRepository.countByWorkspace(workspaceId);
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        List<DocumentRecord> items = documentMetadataRepository.findByWorkspace(workspaceId, safePage, safeSize);
        return new PageResult<>(items, total, safePage, safeSize, totalPages);
    }

    @GetMapping("/{workspaceId}/latest")
    public List<DocumentRecord> listLatestDocuments(@PathVariable String workspaceId,
                                                    @RequestParam String userId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return List.of();
        }
        return documentMetadataRepository.findLatestByWorkspace(workspaceId, page, size);
    }

    @GetMapping("/{workspaceId}/{documentId}/versions")
    public List<DocumentVersionVO> listVersions(@PathVariable String workspaceId,
                                                @PathVariable String documentId,
                                                @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return List.of();
        }
        DocumentRecord record = documentMetadataRepository.findById(documentId)
                .filter(doc -> Objects.equals(doc.workspaceId(), workspaceId))
                .orElse(null);
        if (record == null) {
            return List.of();
        }
        String parentDocumentId = record.parentDocumentId() != null ? record.parentDocumentId() : record.documentId();
        return documentMetadataRepository.findVersionsByParent(workspaceId, parentDocumentId)
                .stream()
                .map(this::toVersionVO)
                .toList();
    }

    @GetMapping("/{workspaceId}/{documentId}/status")
    public Result getStatus(@PathVariable String workspaceId,
                            @PathVariable String documentId,
                            @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return Result.fail("无权限");
        }
        return documentMetadataRepository.findById(documentId)
                .filter(record -> Objects.equals(record.workspaceId(), workspaceId))
                .map(this::toStatusVO)
                .map(this::formatStatusResult)
                .orElseGet(() -> Result.fail("文档不存在"));
    }

    @GetMapping("/{workspaceId}/{documentId}/status/detail")
    public DocumentStatusVO getStatusDetail(@PathVariable String workspaceId,
                                            @PathVariable String documentId,
                                            @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return new DocumentStatusVO(documentId, workspaceId, null, null, null, null, null, null);
        }
        return documentMetadataRepository.findById(documentId)
                .filter(record -> Objects.equals(record.workspaceId(), workspaceId))
                .map(this::toStatusVO)
                .orElseGet(() -> new DocumentStatusVO(documentId, workspaceId, null, null, null, null, null, null));
    }

    @PostMapping(value = "/{workspaceId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result upload(@PathVariable String workspaceId,
                         @RequestParam String userId,
                         @RequestParam(value = "uploadedBy", defaultValue = "system") String uploadedBy,
                         @RequestParam("file") MultipartFile file) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_UPLOAD_DENIED", null, "无权限");
            return Result.fail("无权限");
        }
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            auditLogRepository.save(workspaceId, uploadedBy, "DOCUMENT_UPLOAD_REJECTED", null, "only pdf allowed");
            return Result.fail("只能上传PDF文件！");
        }
        Result result = documentIngestionService.uploadPdf(
                workspaceId,
                uploadedBy,
                Objects.requireNonNull(file.getOriginalFilename(), "filename"),
                file.getContentType(),
                file.getSize(),
                file.getResource()
        );
        auditLogRepository.save(workspaceId, uploadedBy, "DOCUMENT_UPLOAD", null, Objects.requireNonNull(file.getOriginalFilename()));
        return result;
    }

    @PostMapping(value = "/{workspaceId}/upload-version/{parentDocumentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadNewVersion(@PathVariable String workspaceId,
                                   @PathVariable String parentDocumentId,
                                   @RequestParam String userId,
                                   @RequestParam(value = "uploadedBy", defaultValue = "system") String uploadedBy,
                                   @RequestParam("file") MultipartFile file) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_VERSION_UPLOAD_DENIED", parentDocumentId, "无权限");
            return Result.fail("无权限");
        }
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            auditLogRepository.save(workspaceId, uploadedBy, "DOCUMENT_VERSION_UPLOAD_REJECTED", parentDocumentId, "only pdf allowed");
            return Result.fail("只能上传PDF文件！");
        }
        auditLogRepository.save(workspaceId, uploadedBy, "DOCUMENT_VERSION_UPLOAD", parentDocumentId, Objects.requireNonNull(file.getOriginalFilename(), "filename"));
        return documentIngestionService.uploadNewVersion(
                workspaceId,
                parentDocumentId,
                uploadedBy,
                Objects.requireNonNull(file.getOriginalFilename(), "filename"),
                file.getContentType(),
                file.getSize(),
                file.getResource()
        );
    }

    @PostMapping("/{workspaceId}/{documentId}/retry")
    public Result retry(@PathVariable String workspaceId,
                        @PathVariable String documentId,
                        @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_RETRY_DENIED", documentId, "无权限");
            return Result.fail("无权限");
        }
        DocumentRecord record = documentMetadataRepository.findById(documentId)
                .filter(doc -> Objects.equals(doc.workspaceId(), workspaceId))
                .orElse(null);
        if (record == null) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_RETRY_FAILED", documentId, "文档不存在");
            return Result.fail("文档不存在");
        }
        if (!Boolean.TRUE.equals(toStatusVO(record).isCanRetry())) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_RETRY_FAILED", documentId, "当前状态不允许重试");
            return Result.fail("当前状态不允许重试");
        }
        documentMetadataRepository.clearFailureReason(documentId);
        auditLogRepository.save(workspaceId, userId, "DOCUMENT_RETRY", documentId, record.originalFilename());
        Result result = documentIngestionService.reindex(workspaceId, documentId, new org.springframework.core.io.FileSystemResource(record.storagePath()));
        if (result.getOk() != null && result.getOk() == 1) {
            documentMetadataRepository.clearFailureReason(documentId);
        }
        return result;
    }

    @DeleteMapping("/{workspaceId}/{documentId}")
    public Result delete(@PathVariable String workspaceId, @PathVariable String documentId,
                         @RequestParam String userId,
                         @RequestParam(value = "actor", defaultValue = "system") String actor) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_DELETE_DENIED", documentId, "无权限");
            return Result.fail("无权限");
        }
        DocumentRecord record = documentMetadataRepository.findById(documentId)
                .filter(doc -> Objects.equals(doc.workspaceId(), workspaceId))
                .orElse(null);
        if (record == null) {
            auditLogRepository.save(workspaceId, actor, "DOCUMENT_DELETE_FAILED", documentId, "文档不存在");
            return Result.fail("文档不存在");
        }
        if (record.status() != DocumentStatus.DELETED && record.status() != DocumentStatus.FAILED && record.status() != DocumentStatus.INDEXED) {
            auditLogRepository.save(workspaceId, actor, "DOCUMENT_DELETE_FAILED", documentId, "当前状态不允许删除");
            return Result.fail("当前状态不允许删除");
        }
        vectorDocumentRepository.deleteByWorkspaceAndDocument(workspaceId, documentId);

        Resource resource = fileRepository.getFile(workspaceId, documentId);
        if (resource.exists()) {
            try {
                java.io.File file = resource.getFile();
                if (file.exists() && !file.delete()) {
                    log.warn("Failed to delete physical file for document {} in workspace {}", documentId, workspaceId);
                }
            } catch (Exception e) {
                log.warn("Failed to remove physical file for document {} in workspace {}", documentId, workspaceId, e);
            }
        }

        documentMetadataRepository.forceUpdateStatus(documentId, DocumentStatus.DELETED, null);
        auditLogRepository.save(workspaceId, actor, "DOCUMENT_DELETE", documentId, record.originalFilename());
        return Result.ok("删除成功");
    }

    @PostMapping("/{workspaceId}/{documentId}/reindex")
    public Result reindex(@PathVariable String workspaceId,
                          @PathVariable String documentId,
                          @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_REINDEX_DENIED", documentId, "无权限");
            return Result.fail("无权限");
        }
        DocumentRecord record = documentMetadataRepository.findById(documentId)
                .filter(doc -> Objects.equals(doc.workspaceId(), workspaceId))
                .orElse(null);
        if (record == null) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_REINDEX_FAILED", documentId, "文档不存在");
            return Result.fail("文档不存在");
        }
        if (record.status() != DocumentStatus.FAILED && record.status() != DocumentStatus.INDEXED) {
            auditLogRepository.save(workspaceId, userId, "DOCUMENT_REINDEX_FAILED", documentId, "当前状态不允许重建索引");
            return Result.fail("当前状态不允许重建索引");
        }
        auditLogRepository.save(workspaceId, userId, "DOCUMENT_REINDEX", documentId, record.originalFilename());
        return documentIngestionService.reindex(workspaceId, documentId, new org.springframework.core.io.FileSystemResource(record.storagePath()));
    }

    private DocumentStatusVO toStatusVO(DocumentRecord record) {
        return new DocumentStatusVO(
                record.documentId(),
                record.workspaceId(),
                record.originalFilename(),
                record.status(),
                record.failureReason(),
                record.uploadedBy(),
                record.createdAt() == null ? null : record.createdAt().format(DATETIME_FORMATTER),
                record.updatedAt() == null ? null : record.updatedAt().format(DATETIME_FORMATTER)
        );
    }

    private Result formatStatusResult(DocumentStatusVO vo) {
        StringBuilder builder = new StringBuilder();
        builder.append(vo.getStatus()).append("|").append(vo.getStatusDescription());
        if (vo.getFailureReason() != null) {
            builder.append("|").append(vo.getFailureReason()).append("|").append(vo.getFailureReasonDescription());
        }
        builder.append("|canRetry=").append(vo.isCanRetry());
        builder.append("|suggestion=").append(vo.getSuggestion());
        return Result.ok(builder.toString());
    }

    private DocumentVersionVO toVersionVO(DocumentRecord record) {
        return new DocumentVersionVO(
                record.documentId(),
                record.parentDocumentId(),
                record.versionNo(),
                record.latest(),
                record.originalFilename(),
                record.status().name(),
                record.status().getDescription(),
                record.createdAt() == null ? null : record.createdAt().format(DATETIME_FORMATTER)
        );
    }
}
