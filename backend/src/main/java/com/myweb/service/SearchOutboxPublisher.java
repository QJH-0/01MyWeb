package com.myweb.service;

import com.myweb.entity.SearchOutboxEvent;
import com.myweb.entity.SearchSyncEventType;
import com.myweb.repository.SearchOutboxEventRepository;
import org.springframework.stereotype.Component;

@Component
public class SearchOutboxPublisher implements BlogSearchSyncPublisher, ProjectSearchSyncPublisher {

    private static final String AGGREGATE_BLOG = "BLOG";
    private static final String AGGREGATE_PROJECT = "PROJECT";

    private final SearchOutboxEventRepository searchOutboxEventRepository;

    public SearchOutboxPublisher(SearchOutboxEventRepository searchOutboxEventRepository) {
        this.searchOutboxEventRepository = searchOutboxEventRepository;
    }

    @Override
    public void publishUpsert(Long blogId) {
        saveEvent(AGGREGATE_BLOG, blogId, SearchSyncEventType.UPSERT);
    }

    @Override
    public void publishDelete(Long blogId) {
        saveEvent(AGGREGATE_BLOG, blogId, SearchSyncEventType.DELETE);
    }

    @Override
    public void publishProjectUpsert(Long projectId) {
        saveEvent(AGGREGATE_PROJECT, projectId, SearchSyncEventType.UPSERT);
    }

    @Override
    public void publishProjectDelete(Long projectId) {
        saveEvent(AGGREGATE_PROJECT, projectId, SearchSyncEventType.DELETE);
    }

    private void saveEvent(String aggregateType, Long aggregateId, SearchSyncEventType eventType) {
        SearchOutboxEvent event = new SearchOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(buildPayload(aggregateType, aggregateId, eventType));
        searchOutboxEventRepository.save(event);
    }

    private String buildPayload(String aggregateType, Long aggregateId, SearchSyncEventType eventType) {
        return "{\"aggregateType\":\"" + aggregateType + "\",\"aggregateId\":" + aggregateId
            + ",\"eventType\":\"" + eventType.name() + "\"}";
    }
}
