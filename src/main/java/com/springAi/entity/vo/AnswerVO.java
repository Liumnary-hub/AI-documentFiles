package com.springAi.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class AnswerVO {
    private String answer;
    private List<SourceVO> sources;

    public AnswerVO(String answer, List<SourceVO> sources) {
        this.answer = answer;
        this.sources = sources;
    }
}
