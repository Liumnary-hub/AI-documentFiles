package com.springAi.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RoleRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public List<String> findRolesByUserId(String userId) {
        return jdbcTemplate.queryForList(
                "SELECT role_name FROM user_role WHERE user_id = ?",
                String.class,
                userId
        );
    }

    public boolean hasWorkspaceAccess(String userId, String workspaceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM workspace_member WHERE user_id = ? AND workspace_id = ?",
                Integer.class,
                userId,
                workspaceId
        );
        return count != null && count > 0;
    }
}
