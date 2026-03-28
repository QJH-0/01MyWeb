package com.myweb.backend.health;

/** 前端/运维面板消费的汇总状态；字段名变更需同步前端 health API 类型。 */
public record HealthStatus(
        boolean mysql,
        boolean redis,
        boolean elasticsearch,
        boolean minio,
        String elasticsearchStatus
) {
}
