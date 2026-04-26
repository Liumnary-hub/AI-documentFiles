package com.springAi.controller;

import com.springAi.entity.vo.Result;
import com.springAi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/permissions")
public class PermissionController {

    private final RoleRepository roleRepository;

    @GetMapping("/{userId}/roles")
    public List<String> roles(@PathVariable String userId) {
        return roleRepository.findRolesByUserId(userId);
    }

    @GetMapping("/{userId}/workspace/{workspaceId}")
    public Result workspaceAccess(@PathVariable String userId,
                                  @PathVariable String workspaceId) {
        boolean allowed = roleRepository.hasWorkspaceAccess(userId, workspaceId);
        return allowed ? Result.ok("allowed") : Result.fail("forbidden");
    }
}
