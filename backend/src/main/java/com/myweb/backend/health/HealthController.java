package com.myweb.backend.health;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthProbeService healthProbeService;

    public HealthController(HealthProbeService healthProbeService) {
        this.healthProbeService = healthProbeService;
    }

    @GetMapping
    public ApiResponse<HealthStatus> getHealth(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        String traceId = traceAttr == null ? "" : traceAttr.toString();
        HealthStatus data = healthProbeService.probeAll();
        return ApiResponse.ok(data, traceId);
    }
}
