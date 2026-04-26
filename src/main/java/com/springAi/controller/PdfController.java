package com.springAi.controller;

import com.springAi.entity.vo.AnswerVO;
import com.springAi.entity.vo.Result;
import com.springAi.entity.vo.SourceVO;
import com.springAi.repository.ChatHistoryRepository;
import com.springAi.repository.WorkspaceMemberRepository;
import com.springAi.service.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    private final ChatClient pdfChatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final HybridSearchService hybridSearchService;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestParam String prompt,
                             @RequestParam String workspaceId,
                             @RequestParam String conversationId,
                             @RequestParam String userId) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return Flux.just("无权限");
        }
        chatHistoryRepository.save("pdf", conversationId, workspaceId);

        String filterExpression = "workspaceId == '" + workspaceId + "'";
        return pdfChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression)
                )
                .stream()
                .content();
    }

    @GetMapping("/ping")
    public Result ping() {
        return Result.ok("pdf controller ready");
    }

    @GetMapping("/answer")
    public AnswerVO answer(@RequestParam String workspaceId,
                           @RequestParam String userId,
                           @RequestParam String query,
                           @RequestParam(defaultValue = "hybrid") String mode) {
        if (!workspaceMemberRepository.isMember(workspaceId, userId)) {
            return new AnswerVO("无权限", List.of());
        }

        String filterExpression = "workspaceId == '" + workspaceId + "'";
        ChatResponse response = pdfChatClient.prompt()
                .user(query)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .call()
                .chatResponse();

        List<SourceVO> sources;
        if ("vector".equalsIgnoreCase(mode)) {
            // 仅向量检索（用于对比）
            sources = hybridSearchService.vectorSearch(workspaceId, query, 6);
        } else {
            // 默认 hybrid
            sources = hybridSearchService.hybridSearch(workspaceId, query, 6);
        }

        String answer = "";
        if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
            answer = response.getResult().getOutput().getText();
        }
        return new AnswerVO(answer, sources);
    }
}