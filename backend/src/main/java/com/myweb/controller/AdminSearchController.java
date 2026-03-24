package com.myweb.controller;

import com.myweb.common.ApiResponse;
import com.myweb.service.SearchIndexService;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
public class AdminSearchController {
    private final SearchIndexService searchIndexService;

    public AdminSearchController(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    @PostMapping("/consume")
    public ResponseEntity<ApiResponse<Map<String, Object>>> consume() {
        int count = searchIndexService.processPendingEvents();
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed", count), traceId));
    }

    @PostMapping("/rebuild")
    public ResponseEntity<ApiResponse<Object>> rebuild() {
        searchIndexService.rebuildAll();
        String traceId = MDC.get("traceId");
        return ResponseEntity.ok(ApiResponse.ok(null, traceId));
    }
}
