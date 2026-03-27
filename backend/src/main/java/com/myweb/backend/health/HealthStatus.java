package com.myweb.backend.health;

public record HealthStatus(
        boolean mysql,
        boolean redis,
        boolean elasticsearch,
        boolean minio,
        String elasticsearchStatus
) {
}
