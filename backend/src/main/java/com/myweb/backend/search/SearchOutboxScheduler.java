package com.myweb.backend.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时拉取 pending outbox；测试环境可关闭避免后台干扰。 */
@Component
@ConditionalOnProperty(name = "app.search.outbox.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SearchOutboxScheduler {

    private final SearchOutboxService searchOutboxService;
    private final SearchProperties searchProperties;

    public SearchOutboxScheduler(SearchOutboxService searchOutboxService, SearchProperties searchProperties) {
        this.searchOutboxService = searchOutboxService;
        this.searchProperties = searchProperties;
    }

    @Scheduled(fixedDelayString = "${app.search.outbox.poll-ms:5000}")
    public void consumePending() {
        searchOutboxService.processBatch(searchProperties.getOutbox().getBatchSize());
    }
}
