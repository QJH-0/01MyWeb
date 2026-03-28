package com.myweb.backend.search;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.SearchItemDTO;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** ES 关闭或禁用时：写操作为空实现，查询显式失败，避免静默返回假成功。 */
public class DisabledSearchIndexService implements SearchIndexOperations {

    @Override
    public void ensureIndex() {
        // no-op
    }

    @Override
    public void deleteDocument(String sourceType, long sourceId) {
        // no-op
    }

    @Override
    public void indexBlogDocument(
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
    ) {
        // no-op
    }

    @Override
    public void indexProjectDocument(
            long id,
            String title,
            String summary,
            String description,
            String category,
            List<String> tags,
            String url,
            Instant createdAt,
            Instant updatedAt
    ) {
        // no-op
    }

    @Override
    public PagedResult<SearchItemDTO> search(String query, String typeFilter, int page, int limit) {
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Search index unavailable");
    }

    @Override
    public void deleteIndex() {
        // no-op
    }
}
