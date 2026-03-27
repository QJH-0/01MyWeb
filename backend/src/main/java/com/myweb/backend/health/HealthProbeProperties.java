package com.myweb.backend.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
