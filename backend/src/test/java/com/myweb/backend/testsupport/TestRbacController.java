package com.myweb.backend.testsupport;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 仅测试 profile：模拟 AI/搜索路由供 Security 与限流断言使用。 */
@RestController
@RequestMapping("/api")
public class TestRbacController {
    @GetMapping("/ai/chat/stream")
    public ApiResponse<String> aiChatStream(HttpServletRequest request) {
        return ApiResponse.ok("ai-ok", traceId(request));
    }

    @PostMapping("/comments/test")
    public ApiResponse<String> commentWrite(HttpServletRequest request) {
        return ApiResponse.ok("comment-write-ok", traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}

