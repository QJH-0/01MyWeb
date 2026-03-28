package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.SearchItemDTO;
import com.myweb.backend.search.SearchIndexOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 公开搜索：参数校验后委托 ES 实现。 */
@Service
public class SearchService {

    private static final int MAX_LIMIT = 100;

    private final SearchIndexOperations searchIndex;

    public SearchService(SearchIndexOperations searchIndex) {
        this.searchIndex = searchIndex;
    }

    public PagedResult<SearchItemDTO> search(String q, String type, int page, int limit) {
        if (q == null || q.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "q is required");
        }
        if (type != null && !type.isBlank()) {
            String t = type.trim();
            if (!"blog".equals(t) && !"project".equals(t)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid type filter");
            }
        }
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid pagination params");
        }
        String typeFilter = type == null || type.isBlank() ? null : type.trim();
        return searchIndex.search(q.trim(), typeFilter, page, limit);
    }
}
