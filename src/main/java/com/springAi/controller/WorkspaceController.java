package com.springAi.controller;

import com.springAi.entity.vo.Result;
import com.springAi.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/workspaces")
public class WorkspaceController {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @GetMapping("/{workspaceId}/members/{userId}/check")
    public Result checkMember(@PathVariable String workspaceId, @PathVariable String userId) {
        boolean member = workspaceMemberRepository.isMember(workspaceId, userId);
        return member ? Result.ok("member") : Result.fail("not member");
    }

    @PostMapping("/{workspaceId}/members")
    public Result addMember(@PathVariable String workspaceId,
                            @RequestParam String userId,
                            @RequestParam(defaultValue = "member") String memberRole) {
        workspaceMemberRepository.addMember(workspaceId, userId, memberRole);
        return Result.ok("added");
    }
}
