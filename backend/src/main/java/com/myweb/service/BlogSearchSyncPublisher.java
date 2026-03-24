package com.myweb.service;

public interface BlogSearchSyncPublisher {
    void publishUpsert(Long blogId);
    void publishDelete(Long blogId);
}
