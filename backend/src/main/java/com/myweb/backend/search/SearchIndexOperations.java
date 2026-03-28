package com.myweb.backend.search;

import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.SearchItemDTO;

import java.time.Instant;
import java.util.List;

/** ES 索引与检索抽象：便于测试桩与「关闭 ES」实现切换。 */
public interface SearchIndexOperations {

    void ensureIndex();

    void deleteDocument(String sourceType, long sourceId);

    void indexBlogDocument(
            long id,
            String title,
            String summary,
            String content,
            String category,
            List<String> tags,
            String url,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    );

    void indexProjectDocument(
            long id,
            String title,
            String summary,
            String description,
            String category,
            List<String> tags,
            String url,
            Instant createdAt,
            Instant updatedAt
    );

    PagedResult<SearchItemDTO> search(String query, String typeFilter, int page, int limit);

    void deleteIndex();
}
