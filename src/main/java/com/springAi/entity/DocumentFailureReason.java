package com.springAi.entity;

public enum DocumentFailureReason {
    FILE_NOT_FOUND("文件不存在"),
    PARSE_FAILED("解析失败"),
    INDEX_FAILED("索引失败"),
    PERMISSION_DENIED("无权限"),
    STATE_NOT_ALLOWED("当前状态不允许"),
    SAVE_FAILED("保存失败"),
    UNKNOWN_ERROR("未知错误");

    private final String description;

    DocumentFailureReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
