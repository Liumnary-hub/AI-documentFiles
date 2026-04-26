package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class FailureReasonStatVO {
    private String reason;
    private String reasonDescription;
    private long count;

    public FailureReasonStatVO(String reason, String reasonDescription, long count) {
        this.reason = reason;
        this.reasonDescription = reasonDescription;
        this.count = count;
    }
}
