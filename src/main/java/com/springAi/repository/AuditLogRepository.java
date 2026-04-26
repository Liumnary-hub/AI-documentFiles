package com.springAi.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public void save(String workspaceId, String actor, String action, String targetId, String detail) {
        String sql = """
            INSERT INTO audit_log (workspace_id, actor, action, target_id, detail, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
        """;
        jdbcTemplate.update(sql, workspaceId, actor, action, targetId, detail);
    }

    public List<AuditLogRecord> listByWorkspace(String workspaceId) {
        return listByWorkspace(workspaceId, null, null);
    }

    public List<AuditLogRecord> listByWorkspace(String workspaceId, String action, String actor) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE workspace_id = ?");
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = '").append(action.replace("'", "''")).append("'");
        }
        if (actor != null && !actor.isBlank()) {
            sql.append(" AND actor = '").append(actor.replace("'", "''")).append("'");
        }
        sql.append(" ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), this::mapRow, workspaceId);
    }

    public List<DailyAuditCountRecord> countByWorkspaceAndDay(String workspaceId) {
        String sql = """
            SELECT DATE(created_at) AS day, COUNT(1) AS cnt
            FROM audit_log
            WHERE workspace_id = ?
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at) DESC
        """;
        return jdbcTemplate.query(sql, this::mapDailyCountRow, workspaceId);
    }

    private AuditLogRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuditLogRecord(
                rs.getLong("id"),
                rs.getString("workspace_id"),
                rs.getString("actor"),
                rs.getString("action"),
                rs.getString("target_id"),
                rs.getString("detail"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private DailyAuditCountRecord mapDailyCountRow(ResultSet rs, int rowNum) throws SQLException {
        LocalDate day = rs.getDate("day") == null ? null : rs.getDate("day").toLocalDate();
        long count = rs.getLong("cnt");
        return new DailyAuditCountRecord(day, count);
    }

    public record AuditLogRecord(
            long id,
            String workspaceId,
            String actor,
            String action,
            String targetId,
            String detail,
            LocalDateTime createdAt
    ) {
    }

    public record DailyAuditCountRecord(LocalDate day, long count) {
    }
}
