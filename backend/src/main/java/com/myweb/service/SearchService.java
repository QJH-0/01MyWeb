package com.myweb.service;

import com.myweb.common.PagedResult;
import com.myweb.dto.SearchItemDTO;
import com.myweb.entity.SearchDocument;
import com.myweb.repository.SearchDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private final SearchDocumentRepository searchDocumentRepository;

    public SearchService(SearchDocumentRepository searchDocumentRepository) {
        this.searchDocumentRepository = searchDocumentRepository;
    }

    public PagedResult<SearchItemDTO> search(String keyword, String sourceType, int page, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("关键词不能为空");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page 不能小于 0");
        }
        if (limit <= 0 || limit > 50) {
            throw new IllegalArgumentException("limit 必须在 1 到 50 之间");
        }
        if (sourceType != null && !sourceType.isBlank()) {
            String value = sourceType.trim().toLowerCase();
            if (!"blog".equals(value) && !"project".equals(value)) {
                throw new IllegalArgumentException("type 仅支持 blog 或 project");
            }
        }

        String normalizedKeyword = keyword.trim();
        String normalizedType = sourceType == null ? "" : sourceType.trim().toLowerCase();
        Pageable pageable = PageRequest.of(page, limit);
        Page<SearchDocument> result;
        if (normalizedType.isBlank()) {
            result = searchDocumentRepository.findByContentContainingIgnoreCase(normalizedKeyword, pageable);
        } else {
            result = searchDocumentRepository.findBySourceTypeAndContentContainingIgnoreCase(
                normalizedType, normalizedKeyword, pageable
            );
        }

        List<SearchItemDTO> items = new ArrayList<>();
        for (SearchDocument doc : result.getContent()) {
            SearchItemDTO dto = new SearchItemDTO();
            dto.setSourceType(doc.getSourceType());
            dto.setSourceId(doc.getSourceId());
            dto.setTitle(doc.getTitle());
            dto.setUrl(doc.getUrl());
            dto.setSnippet(buildSnippet(doc.getContent(), normalizedKeyword));
            items.add(dto);
        }
        return new PagedResult<>(items, result.getTotalElements(), page, limit);
    }

    private String buildSnippet(String content, String keyword) {
        String lowerContent = content.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerContent.indexOf(lowerKeyword);
        if (index < 0) {
            return content.substring(0, Math.min(content.length(), 120));
        }
        int start = Math.max(0, index - 40);
        int end = Math.min(content.length(), index + keyword.length() + 40);
        String raw = content.substring(start, end);
        return raw.replaceAll("(?i)" + java.util.regex.Pattern.quote(keyword), "<em>$0</em>");
    }
}
