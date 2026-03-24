package com.myweb.repository;

import com.myweb.entity.SearchDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchDocumentRepository extends JpaRepository<SearchDocument, String> {
    Page<SearchDocument> findBySourceTypeAndContentContainingIgnoreCase(
        String sourceType, String keyword, Pageable pageable
    );

    Page<SearchDocument> findByContentContainingIgnoreCase(String keyword, Pageable pageable);
}
