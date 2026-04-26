package com.springAi.repository;

import org.springframework.core.io.Resource;

public interface FileRepository {

    /**
     * 保存企业文档，并记录知识库与文档的映射关系。
     *
     * @param workspaceId 知识库空间 ID
     * @param documentId 文档 ID
     * @param resource 文档资源
     * @return 保存成功返回 true，否则返回 false
     */
    boolean save(String workspaceId, String documentId, Resource resource);

    /**
     * 根据知识库空间与文档 ID 获取文件。
     *
     * @param workspaceId 知识库空间 ID
     * @param documentId 文档 ID
     * @return 找到的文件
     */
    Resource getFile(String workspaceId, String documentId);

    /**
     * 兼容旧接口：默认将 chatId 视作 workspaceId/documentId。
     */
    @Deprecated
    default boolean save(String chatId, Resource resource) {
        return save(chatId, chatId, resource);
    }

    /**
     * 兼容旧接口：默认将 chatId 视作 workspaceId/documentId。
     */
    @Deprecated
    default Resource getFile(String chatId) {
        return getFile(chatId, chatId);
    }
}