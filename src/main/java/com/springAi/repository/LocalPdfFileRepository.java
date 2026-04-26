package com.springAi.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPdfFileRepository implements FileRepository {

    private static final String STORAGE_DIR = "enterprise-docs";
    private static final String MAPPING_FILE = "enterprise-docs.properties";

    // workspaceId:documentId -> file path
    private final Properties documentFiles = new Properties();

    @Override
    public boolean save(String workspaceId, String documentId, Resource resource) {
        String filename = Objects.requireNonNull(resource.getFilename());
        String safeWorkspaceId = sanitize(workspaceId);
        String safeDocumentId = sanitize(documentId);

        try {
            Files.createDirectories(Path.of(STORAGE_DIR, safeWorkspaceId));
            Path targetPath = Path.of(STORAGE_DIR, safeWorkspaceId, safeDocumentId + "-" + filename);
            if (!Files.exists(targetPath)) {
                Files.copy(resource.getInputStream(), targetPath);
            }
            documentFiles.put(key(workspaceId, documentId), targetPath.toString());
            return true;
        } catch (IOException e) {
            log.error("Failed to save document resource.", e);
            return false;
        }
    }

    @Override
    public Resource getFile(String workspaceId, String documentId) {
        String path = documentFiles.getProperty(key(workspaceId, documentId));
        return path == null ? new FileSystemResource(new File("") ) : new FileSystemResource(path);
    }

    @PostConstruct
    private void init() {
        FileSystemResource mappingResource = new FileSystemResource(MAPPING_FILE);
        if (mappingResource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mappingResource.getInputStream(), StandardCharsets.UTF_8))) {
                documentFiles.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load enterprise-docs.properties", e);
            }
        }
    }

    @PreDestroy
    private void persistent() {
        try (FileWriter writer = new FileWriter(MAPPING_FILE)) {
            documentFiles.store(writer, LocalDateTime.now().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save enterprise-docs.properties", e);
        }
    }

    private String key(String workspaceId, String documentId) {
        return sanitize(workspaceId) + ":" + sanitize(documentId);
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}