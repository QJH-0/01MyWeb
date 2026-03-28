package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.ContentPageDTO;
import com.myweb.backend.service.ContentQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开 CMS 内容页：按固定 key（home/about/experience）映射存储，供门户静态页渲染。 */
@RestController
@RequestMapping("/api/content")
public class ContentPublicController {
    private final ContentQueryService contentQueryService;

    public ContentPublicController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    @GetMapping("/home")
    public ApiResponse<ContentPageDTO> home(HttpServletRequest httpRequest) {
        return ApiResponse.ok(contentQueryService.getPageByKey("home"), traceId(httpRequest));
    }

    @GetMapping("/about")
    public ApiResponse<ContentPageDTO> about(HttpServletRequest httpRequest) {
        return ApiResponse.ok(contentQueryService.getPageByKey("about"), traceId(httpRequest));
    }

    @GetMapping("/experience")
    public ApiResponse<ContentPageDTO> experience(HttpServletRequest httpRequest) {
        return ApiResponse.ok(contentQueryService.getPageByKey("experience"), traceId(httpRequest));
    }

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串，若不存在则返回空字符串
     */
    private String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return value == null ? "" : value.toString();
    }
}
