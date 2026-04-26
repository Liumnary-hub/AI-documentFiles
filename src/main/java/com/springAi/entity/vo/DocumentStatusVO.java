package com.springAi.entity.vo;

import com.springAi.entity.DocumentFailureReason;
import com.springAi.entity.DocumentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DocumentStatusVO {
    private String documentId;
    private String workspaceId;
    private String originalFilename;
    private String status;
    private String statusDescription;
    private String failureReason;
    private String failureReasonDescription;
    private boolean canRetry;
    private String suggestion;
    private String uploadedBy;
    private String createdAt;
    private String updatedAt;

    public DocumentStatusVO(String documentId,
                            String workspaceId,
                            String originalFilename,
                            DocumentStatus status,
                            DocumentFailureReason failureReason,
                            String uploadedBy,
                            String createdAt,
                            String updatedAt) {
        this.documentId = documentId;
        this.workspaceId = workspaceId;
        this.originalFilename = originalFilename;
        this.status = status == null ? null : status.name();
        this.statusDescription = status == null ? null : status.getDescription();
        this.failureReason = failureReason == null ? null : failureReason.name();
        this.failureReasonDescription = failureReason == null ? null : failureReason.getDescription();
        this.canRetry = status == DocumentStatus.FAILED;
        this.suggestion = buildSuggestion(status, failureReason);
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private String buildSuggestion(DocumentStatus status, DocumentFailureReason failureReason) {
        if (status == null) {
            return "无法获取文档状态";
        }
        return switch (status) {
            case UPLOADED -> "文档已上传，等待后台处理";
            case PROCESSING -> "文档正在处理中，请稍后刷新";
            case INDEXED -> "文档已完成索引，可以开始问答";
            case DELETED -> "文档已删除，无法继续使用";
            case FAILED -> {
                if (failureReason == null) {
                    yield "处理失败，请尝试重试";
                }
                yield switch (failureReason) {
                    case FILE_NOT_FOUND -> "文件不存在，请重新上传";
                    case PARSE_FAILED -> "解析失败，建议检查 PDF 是否为扫描件或损坏文件";
                    case INDEX_FAILED -> "索引失败，建议重试或检查向量库连接";
                    case PERMISSION_DENIED -> "无权限访问该文档，请联系管理员";
                    case STATE_NOT_ALLOWED -> "当前状态不允许该操作，请刷新后再试";
                    case SAVE_FAILED -> "文件保存失败，请检查磁盘空间或目录权限";
                    case UNKNOWN_ERROR -> "处理发生未知错误，请查看审计日志";
                };
            }
        };
    }
}
