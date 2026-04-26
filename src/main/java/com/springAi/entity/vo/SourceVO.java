package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Data
public class SourceVO {
    private String content;
    private Map<String, Object> metadata;
    private String documentId;
    private String workspaceId;
    private Integer page;

    public SourceVO(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
        Object workspace = metadata == null ? null : metadata.get("workspaceId");
        Object document = metadata == null ? null : metadata.get("documentId");
        Object pageValue = metadata == null ? null : metadata.get("page_number");
        this.workspaceId = workspace == null ? null : workspace.toString();
        this.documentId = document == null ? null : document.toString();
        this.page = pageValue instanceof Number ? ((Number) pageValue).intValue() : null;
    }
}
