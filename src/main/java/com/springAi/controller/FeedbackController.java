package com.springAi.controller;

import com.springAi.entity.vo.Result;
import com.springAi.repository.DocumentFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/feedback")
public class FeedbackController {

    private final DocumentFeedbackRepository documentFeedbackRepository;

    @PostMapping("/document")
    public Result documentFeedback(@RequestParam String workspaceId,
                                   @RequestParam String conversationId,
                                   @RequestParam(required = false) String documentId,
                                   @RequestParam String prompt,
                                   @RequestParam(required = false) String answer,
                                   @RequestParam int rating,
                                   @RequestParam(required = false) String comment) {
        documentFeedbackRepository.save(workspaceId, conversationId, documentId, prompt, answer, rating, comment);
        return Result.ok("feedback saved");
    }
}
