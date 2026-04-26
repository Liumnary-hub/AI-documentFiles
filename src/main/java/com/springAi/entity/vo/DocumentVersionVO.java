package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DocumentVersionVO {
    private String documentId;
    private String parentDocumentId;
    private int versionNo;
    private boolean latest;
    private String originalFilename;
    private String status;
    private String statusDescription;
    private String createdAt;

    public DocumentVersionVO(String documentId, String parentDocumentId, int versionNo, boolean latest,
                             String originalFilename, String status, String statusDescription, String createdAt) {
        this.documentId = documentId;
        this.parentDocumentId = parentDocumentId;
        this.versionNo = versionNo;
        this.latest = latest;
        this.originalFilename = originalFilename;
        this.status = status;
        this.statusDescription = statusDescription;
        this.createdAt = createdAt;
    }
}
