package com.springAi.entity;

import java.util.EnumSet;
import java.util.Set;

public enum DocumentStatus {
    UPLOADED("已上传"),
    PROCESSING("处理中"),
    INDEXED("已索引"),
    FAILED("失败"),
    DELETED("已删除");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(DocumentStatus target) {
        return switch (this) {
            case UPLOADED -> EnumSet.of(PROCESSING, FAILED, DELETED).contains(target);
            case PROCESSING -> EnumSet.of(INDEXED, FAILED, DELETED).contains(target);
            case INDEXED -> EnumSet.of(DELETED, PROCESSING).contains(target);
            case FAILED -> EnumSet.of(PROCESSING, DELETED).contains(target);
            case DELETED -> false;
        };
    }

    public static boolean isActive(DocumentStatus status) {
        return status != null && status != DELETED;
    }

    public static Set<DocumentStatus> updatableStatuses() {
        return EnumSet.of(UPLOADED, PROCESSING, INDEXED, FAILED);
    }
}
