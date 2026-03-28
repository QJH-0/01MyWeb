package com.myweb.backend.search;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Outbox 入队与批处理：与业务写库同事务入队；消费由调度或管理端触发。 */
@Service
public class SearchOutboxService {

    private static final String PAYLOAD_EMPTY = "{}";

    private final SearchOutboxRepository repository;
    private final SearchOutboxWorker worker;

    public SearchOutboxService(SearchOutboxRepository repository, SearchOutboxWorker worker) {
        this.repository = repository;
        this.worker = worker;
    }

    @Transactional
    public void enqueueBlogUpdated(long blogId) {
        enqueue("blog", blogId, "updated");
    }

    @Transactional
    public void enqueueBlogDeleted(long blogId) {
        enqueue("blog", blogId, "deleted");
    }

    @Transactional
    public void enqueueProjectUpdated(long projectId) {
        enqueue("project", projectId, "updated");
    }

    @Transactional
    public void enqueueProjectDeleted(long projectId) {
        enqueue("project", projectId, "deleted");
    }

    private void enqueue(String aggregateType, long aggregateId, String eventType) {
        SearchOutboxEntity row = new SearchOutboxEntity();
        row.setAggregateType(aggregateType);
        row.setAggregateId(aggregateId);
        row.setEventType(eventType);
        row.setPayload(PAYLOAD_EMPTY);
        row.setStatus(SearchOutboxStatus.PENDING);
        row.setRetryCount(0);
        repository.save(row);
    }

    /** 顺序处理最多 max 条 pending，返回本次标记为 completed 的条数。 */
    public int processBatch(int max) {
        int completed = 0;
        for (int i = 0; i < max; i++) {
            SearchOutboxEntity next = repository.findFirstByStatusOrderByIdAsc(SearchOutboxStatus.PENDING).orElse(null);
            if (next == null) {
                break;
            }
            long id = next.getId();
            worker.processOne(id);
            SearchOutboxEntity after = repository.findById(id).orElseThrow();
            if (after.getStatus() == SearchOutboxStatus.COMPLETED) {
                completed++;
            }
        }
        return completed;
    }
}
