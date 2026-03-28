package com.myweb.backend.health;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 聚合健康检查：探针结果不对外暴露敏感连接串，仅布尔与 ES 文本状态。 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthProbeService healthProbeService;

    public HealthController(HealthProbeService healthProbeService) {
        this.healthProbeService = healthProbeService;
    }

    @GetMapping
    public ApiResponse<HealthStatus> getHealth(HttpServletRequest request) {
        String traceId = traceId(request);
        HealthStatus data = healthProbeService.probeAll();
        return ApiResponse.ok(data, traceId);
    }

    /**
     * 从请求属性中获取 traceId。
     *
     * @param request HTTP 请求
     * @return traceId 字符串，若不存在则返回空字符串
     */
    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}
