package com.myweb.backend.search;

/** Outbox 行状态：与阶段文档 pending/processing/completed/failed 对齐。 */
public enum SearchOutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
