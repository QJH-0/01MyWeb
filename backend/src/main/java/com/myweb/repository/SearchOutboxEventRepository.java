package com.myweb.repository;

import com.myweb.entity.SearchOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchOutboxEventRepository extends JpaRepository<SearchOutboxEvent, Long> {
    List<SearchOutboxEvent> findAllByAggregateTypeOrderByIdAsc(String aggregateType);
    List<SearchOutboxEvent> findTop100ByStatusOrderByIdAsc(String status);
}
