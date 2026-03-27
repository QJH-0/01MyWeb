package com.myweb.backend.repository;

import com.myweb.backend.entity.ContentPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentPageRepository extends JpaRepository<ContentPageEntity, Long> {
    Optional<ContentPageEntity> findByPageKey(String pageKey);
}

