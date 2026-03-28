package com.myweb.backend.search;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 单条 outbox 独立事务：失败可重试且不污染同批其它行。 */
@Component
public class SearchOutboxWorker {

    private static final int MAX_ERROR_LEN = 2000;

    private final SearchOutboxRepository repository;
    private final SearchOutboxProcessor processor;

    public SearchOutboxWorker(SearchOutboxRepository repository, SearchOutboxProcessor processor) {
        this.repository = repository;
        this.processor = processor;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(long id) {
        SearchOutboxEntity row = repository.findById(id).orElseThrow();
        if (row.getStatus() != SearchOutboxStatus.PENDING) {
            return;
        }
        row.setStatus(SearchOutboxStatus.PROCESSING);
        repository.saveAndFlush(row);
        try {
            processor.process(row);
            row.setStatus(SearchOutboxStatus.COMPLETED);
            row.setProcessedAt(Instant.now());
            row.setErrorMessage(null);
        } catch (RuntimeException ex) {
            int nextRetry = row.getRetryCount() + 1;
            row.setRetryCount(nextRetry);
            String msg = truncate(ex.getMessage());
            if (nextRetry >= 3) {
                row.setStatus(SearchOutboxStatus.FAILED);
                row.setErrorMessage(msg);
            } else {
                row.setStatus(SearchOutboxStatus.PENDING);
                row.setErrorMessage(msg);
            }
        }
        repository.save(row);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= MAX_ERROR_LEN) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LEN);
    }
}
