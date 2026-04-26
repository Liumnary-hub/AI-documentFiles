package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DailyCountVO {
    private String day;
    private long count;

    public DailyCountVO(String day, long count) {
        this.day = day;
        this.count = count;
    }
}
