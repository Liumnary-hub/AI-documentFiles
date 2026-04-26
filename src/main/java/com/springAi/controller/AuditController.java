package com.springAi.controller;

import com.springAi.entity.vo.AuditLogVO;
import com.springAi.entity.vo.DailyCountVO;
import com.springAi.entity.vo.FailureReasonStatVO;
import com.springAi.entity.vo.Result;
import com.springAi.repository.AuditLogRepository;
import com.springAi.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/audit")
public class AuditController {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    @PostMapping("/log")
    public Result log(@RequestParam String workspaceId,
                      @RequestParam String actor,
                      @RequestParam String action,
                      @RequestParam(required = false) String targetId,
                      @RequestParam(required = false) String detail) {
        auditLogRepository.save(workspaceId, actor, action, targetId, detail);
        return Result.ok("logged");
    }

    @GetMapping("/{workspaceId}")
    public List<AuditLogVO> list(@PathVariable String workspaceId,
                                 @RequestParam(required = false) String action,
                                 @RequestParam(required = false) String actor) {
        return auditLogRepository.listByWorkspace(workspaceId, action, actor)
                .stream()
                .map(record -> new AuditLogVO(
                        record.id(),
                        record.workspaceId(),
                        record.actor(),
                        record.action(),
                        record.targetId(),
                        record.detail(),
                        record.createdAt() == null ? null : record.createdAt().format(DATETIME_FORMATTER)
                ))
                .toList();
    }

    @GetMapping("/{workspaceId}/trend")
    public List<DailyCountVO> trend(@PathVariable String workspaceId) {
        return auditLogRepository.countByWorkspaceAndDay(workspaceId)
                .stream()
                .map(record -> new DailyCountVO(
                        record.day() == null ? null : record.day().toString(),
                        record.count()
                ))
                .toList();
    }

    @GetMapping("/{workspaceId}/failure-stats")
    public List<FailureReasonStatVO> failureStats(@PathVariable String workspaceId) {
        return documentMetadataRepository.countFailureReasonsByWorkspace(workspaceId)
                .entrySet()
                .stream()
                .map(entry -> new FailureReasonStatVO(
                        entry.getKey().name(),
                        entry.getKey().getDescription(),
                        entry.getValue()
                ))
                .toList();
    }
}
