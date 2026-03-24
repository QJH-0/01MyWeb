package com.myweb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class NoopBlogSearchSyncPublisher implements BlogSearchSyncPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopBlogSearchSyncPublisher.class);

    @Override
    public void publishUpsert(Long blogId) {
        // M4 接入 ES 双写时替换为真实 outbox/event 实现
        log.info("blog_search_sync=upsert_reserved, blogId={}", blogId);
    }

    @Override
    public void publishDelete(Long blogId) {
        // M4 接入 ES 双写时替换为真实 outbox/event 实现
        log.info("blog_search_sync=delete_reserved, blogId={}", blogId);
    }
}
