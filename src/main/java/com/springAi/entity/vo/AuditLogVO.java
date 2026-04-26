package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AuditLogVO {
    private long id;
    private String workspaceId;
    private String actor;
    private String action;
    private String targetId;
    private String detail;
    private String createdAt;

    public AuditLogVO(long id, String workspaceId, String actor, String action, String targetId, String detail, String createdAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.actor = actor;
        this.action = action;
        this.targetId = targetId;
        this.detail = detail;
        this.createdAt = createdAt;
    }
}
