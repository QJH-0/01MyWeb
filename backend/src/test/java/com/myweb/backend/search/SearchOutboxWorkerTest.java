package com.myweb.backend.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 失败时重试计数与终态：第 3 次失败后标记 FAILED，否则回到 PENDING。 */
@ExtendWith(MockitoExtension.class)
class SearchOutboxWorkerTest {

    @Mock
    private SearchOutboxRepository repository;
    @Mock
    private SearchOutboxProcessor processor;

    @InjectMocks
    private SearchOutboxWorker worker;

    private SearchOutboxEntity row;

    @BeforeEach
    void setUp() {
        row = new SearchOutboxEntity();
        row.setAggregateType("blog");
        row.setAggregateId(1L);
        row.setEventType("updated");
        row.setPayload("{}");
        row.setStatus(SearchOutboxStatus.PENDING);
        row.setRetryCount(0);
    }

    @Test
    void successMarksCompleted() {
        when(repository.findById(99L)).thenReturn(Optional.of(row));
        doNothing().when(processor).process(any(SearchOutboxEntity.class));

        worker.processOne(99L);

        assertThat(row.getStatus()).isEqualTo(SearchOutboxStatus.COMPLETED);
        assertThat(row.getRetryCount()).isEqualTo(0);
        assertThat(row.getErrorMessage()).isNull();
        verify(repository).saveAndFlush(row);
        verify(repository).save(row);
    }

    @Test
    void failureIncrementsRetryAndReturnsToPendingWhenBelowCap() {
        when(repository.findById(99L)).thenReturn(Optional.of(row));
        doThrow(new RuntimeException("index unavailable")).when(processor).process(any(SearchOutboxEntity.class));

        worker.processOne(99L);

        assertThat(row.getRetryCount()).isEqualTo(1);
        assertThat(row.getStatus()).isEqualTo(SearchOutboxStatus.PENDING);
        assertThat(row.getErrorMessage()).contains("index unavailable");
    }

    @Test
    void failureMarksFailedAfterThirdAttempt() {
        row.setRetryCount(2);
        when(repository.findById(99L)).thenReturn(Optional.of(row));
        doThrow(new RuntimeException("still down")).when(processor).process(any(SearchOutboxEntity.class));

        worker.processOne(99L);

        assertThat(row.getRetryCount()).isEqualTo(3);
        assertThat(row.getStatus()).isEqualTo(SearchOutboxStatus.FAILED);
        assertThat(row.getErrorMessage()).contains("still down");
    }
}
