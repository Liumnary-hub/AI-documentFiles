package com.springAi.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceMemberRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public boolean isMember(String workspaceId, String userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM workspace_member WHERE workspace_id = ? AND user_id = ?",
                Integer.class,
                workspaceId,
                userId
        );
        return count != null && count > 0;
    }

    public void addMember(String workspaceId, String userId, String memberRole) {
        String sql = """
            INSERT INTO workspace_member (workspace_id, user_id, member_role, created_at)
            VALUES (?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE member_role = VALUES(member_role)
        """;
        jdbcTemplate.update(sql, workspaceId, userId, memberRole);
    }
}
