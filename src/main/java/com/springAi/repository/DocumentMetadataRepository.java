package com.springAi.repository;

import com.springAi.entity.DocumentFailureReason;
import com.springAi.entity.DocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentMetadataRepository {

    @Qualifier("mysqlJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public void save(DocumentRecord record) {
        String sql = """
            INSERT INTO document_metadata
            (document_id, workspace_id, original_filename, storage_path, content_type, file_size,
             status, failure_reason, uploaded_by, version_no, is_latest, parent_document_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                workspace_id = VALUES(workspace_id),
                original_filename = VALUES(original_filename),
                storage_path = VALUES(storage_path),
                content_type = VALUES(content_type),
                file_size = VALUES(file_size),
                status = VALUES(status),
                failure_reason = VALUES(failure_reason),
                uploaded_by = VALUES(uploaded_by),
                version_no = VALUES(version_no),
                is_latest = VALUES(is_latest),
                parent_document_id = VALUES(parent_document_id),
                updated_at = NOW()
        """;
        jdbcTemplate.update(sql,
                record.documentId(),
                record.workspaceId(),
                record.originalFilename(),
                record.storagePath(),
                record.contentType(),
                record.fileSize(),
                record.status().name(),
                record.failureReason() == null ? null : record.failureReason().name(),
                record.uploadedBy(),
                record.versionNo(),
                record.latest() ? 1 : 0,
                record.parentDocumentId());
    }

    public boolean updateStatus(String documentId, DocumentStatus targetStatus) {
        return updateStatus(documentId, targetStatus, null);
    }

    public boolean updateStatus(String documentId, DocumentStatus targetStatus, DocumentFailureReason failureReason) {
        return findById(documentId)
                .map(record -> {
                    if (!record.status().canTransitionTo(targetStatus)) {
                        return false;
                    }
                    jdbcTemplate.update("UPDATE document_metadata SET status = ?, failure_reason = ?, updated_at = NOW() WHERE document_id = ?",
                            targetStatus.name(), failureReason == null ? null : failureReason.name(), documentId);
                    return true;
                })
                .orElse(false);
    }

    public void forceUpdateStatus(String documentId, DocumentStatus targetStatus) {
        forceUpdateStatus(documentId, targetStatus, null);
    }

    public void forceUpdateStatus(String documentId, DocumentStatus targetStatus, DocumentFailureReason failureReason) {
        jdbcTemplate.update("UPDATE document_metadata SET status = ?, failure_reason = ?, updated_at = NOW() WHERE document_id = ?",
                targetStatus.name(), failureReason == null ? null : failureReason.name(), documentId);
    }

    public boolean clearFailureReason(String documentId) {
        return jdbcTemplate.update("UPDATE document_metadata SET failure_reason = NULL, updated_at = NOW() WHERE document_id = ?",
                documentId) > 0;
    }

    public boolean markAllOlderVersionsNotLatest(String workspaceId, String parentDocumentId) {
        return jdbcTemplate.update("UPDATE document_metadata SET is_latest = 0 WHERE workspace_id = ? AND parent_document_id = ?",
                workspaceId, parentDocumentId) >= 0;
    }

    public boolean markLatest(String documentId) {
        return jdbcTemplate.update("UPDATE document_metadata SET is_latest = 1, updated_at = NOW() WHERE document_id = ?", documentId) > 0;
    }

    public boolean deleteById(String documentId) {
        return jdbcTemplate.update("DELETE FROM document_metadata WHERE document_id = ?", documentId) > 0;
    }

    public Optional<DocumentRecord> findById(String documentId) {
        List<DocumentRecord> list = jdbcTemplate.query(
                "SELECT * FROM document_metadata WHERE document_id = ?",
                this::mapRow,
                documentId
        );
        return list.stream().findFirst();
    }

    public List<DocumentRecord> findByWorkspace(String workspaceId) {
        return findByWorkspace(workspaceId, 1, 20);
    }

    public List<DocumentRecord> findByWorkspace(String workspaceId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return jdbcTemplate.query(
                "SELECT * FROM document_metadata WHERE workspace_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?",
                this::mapRow,
                workspaceId,
                safeSize,
                offset
        );
    }

    public long countByWorkspace(String workspaceId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM document_metadata WHERE workspace_id = ?",
                Long.class,
                workspaceId
        );
        return total == null ? 0L : total;
    }

    public List<DocumentRecord> findLatestByWorkspace(String workspaceId) {
        return findLatestByWorkspace(workspaceId, 1, 20);
    }

    public List<DocumentRecord> findLatestByWorkspace(String workspaceId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        return jdbcTemplate.query(
                "SELECT * FROM document_metadata WHERE workspace_id = ? AND is_latest = 1 ORDER BY created_at DESC LIMIT ? OFFSET ?",
                this::mapRow,
                workspaceId,
                safeSize,
                offset
        );
    }

    public List<DocumentRecord> findVersionsByParent(String workspaceId, String parentDocumentId) {
        return jdbcTemplate.query(
                "SELECT * FROM document_metadata WHERE workspace_id = ? AND parent_document_id = ? ORDER BY version_no DESC",
                this::mapRow,
                workspaceId, parentDocumentId
        );
    }

    public Map<DocumentFailureReason, Long> countFailureReasonsByWorkspace(String workspaceId) {
        try {
            return countFailureReasonsByColumn(workspaceId, "failure_reason");
        } catch (BadSqlGrammarException ex) {
            // 兼容旧库字段名（驼峰）
            return countFailureReasonsByColumn(workspaceId, "failureReason");
        }
    }

    private Map<DocumentFailureReason, Long> countFailureReasonsByColumn(String workspaceId, String columnName) {
        String sql = """
                SELECT %s, COUNT(*) AS cnt
                FROM document_metadata
                WHERE workspace_id = ? AND %s IS NOT NULL
                GROUP BY %s
                """.formatted(columnName, columnName, columnName);

        return jdbcTemplate.query(sql, rs -> {
            Map<DocumentFailureReason, Long> result = new EnumMap<>(DocumentFailureReason.class);
            while (rs.next()) {
                String reason = rs.getString(columnName);
                long count = rs.getLong("cnt");
                try {
                    result.put(DocumentFailureReason.valueOf(reason), count);
                } catch (Exception ignored) {
                    result.merge(DocumentFailureReason.UNKNOWN_ERROR, count, Long::sum);
                }
            }
            return result;
        }, workspaceId);
    }

    private DocumentRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        String failureReason = rs.getString("failure_reason");
        return new DocumentRecord(
                rs.getString("document_id"),
                rs.getString("workspace_id"),
                rs.getString("original_filename"),
                rs.getString("storage_path"),
                rs.getString("content_type"),
                rs.getLong("file_size"),
                DocumentStatus.valueOf(rs.getString("status")),
                failureReason == null ? null : DocumentFailureReason.valueOf(failureReason),
                rs.getString("uploaded_by"),
                rs.getInt("version_no"),
                rs.getInt("is_latest") == 1,
                rs.getString("parent_document_id"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    public record DocumentRecord(
            String documentId,
            String workspaceId,
            String originalFilename,
            String storagePath,
            String contentType,
            long fileSize,
            DocumentStatus status,
            DocumentFailureReason failureReason,
            String uploadedBy,
            int versionNo,
            boolean latest,
            String parentDocumentId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
