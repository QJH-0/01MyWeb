package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.common.PagedResult;
import com.myweb.dto.SearchItemDTO;
import com.myweb.service.SearchService;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResult<SearchItemDTO>>> search(
        @RequestParam(name = "q") String keyword,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(searchService.search(keyword, type, page, limit), traceId));
    }
}
