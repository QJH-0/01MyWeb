package com.myweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "search_outbox_event")
public class SearchOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SearchSyncEventType eventType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false, length = 300)
    private String payload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public SearchSyncEventType getEventType() {
        return eventType;
    }

    public void setEventType(SearchSyncEventType eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
