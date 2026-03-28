package com.myweb.backend.search;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchOutboxRepository extends JpaRepository<SearchOutboxEntity, Long> {

    Optional<SearchOutboxEntity> findFirstByStatusOrderByIdAsc(SearchOutboxStatus status);

    long countByStatus(SearchOutboxStatus status);
}
