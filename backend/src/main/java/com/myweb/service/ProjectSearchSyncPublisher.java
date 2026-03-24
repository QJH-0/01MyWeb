package com.myweb.service;

public interface ProjectSearchSyncPublisher {
    void publishProjectUpsert(Long projectId);
    void publishProjectDelete(Long projectId);
}
