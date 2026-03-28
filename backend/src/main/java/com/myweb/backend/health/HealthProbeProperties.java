package com.myweb.backend.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 各依赖 host/port 与超时：用于启动外读，避免在探针代码中写死地址。 */
@ConfigurationProperties(prefix = "app.probes")
public record HealthProbeProperties(
        Endpoint mysql,
        Endpoint redis,
        Endpoint elasticsearch,
        Endpoint minio,
        int connectTimeoutMs,
        int readTimeoutMs
) {
    public record Endpoint(
            String host,
            int port
    ) {
    }
}
