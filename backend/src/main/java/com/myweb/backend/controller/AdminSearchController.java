package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.SearchConsumeResultDTO;
import com.myweb.backend.search.SearchOutboxService;
import com.myweb.backend.search.SearchProperties;
import com.myweb.backend.search.SearchRebuildService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端搜索运维：消费 outbox、全量重建索引。 */
@RestController
@RequestMapping("/api/admin/search")
public class AdminSearchController {

    private final SearchOutboxService searchOutboxService;
    private final SearchRebuildService searchRebuildService;
    private final SearchProperties searchProperties;

    public AdminSearchController(
            SearchOutboxService searchOutboxService,
            SearchRebuildService searchRebuildService,
            SearchProperties searchProperties
    ) {
        this.searchOutboxService = searchOutboxService;
        this.searchRebuildService = searchRebuildService;
        this.searchProperties = searchProperties;
    }

    @PostMapping("/consume")
    public ApiResponse<SearchConsumeResultDTO> consume(HttpServletRequest httpRequest) {
        int n = searchOutboxService.processBatch(searchProperties.getOutbox().getBatchSize());
        return ApiResponse.ok(new SearchConsumeResultDTO(n), traceId(httpRequest));
    }

    @PostMapping("/rebuild")
    public ApiResponse<Void> rebuild(HttpServletRequest httpRequest) {
        searchRebuildService.rebuildAll();
        return ApiResponse.ok(null, traceId(httpRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}
