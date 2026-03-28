package com.myweb.backend.repository;

import com.myweb.backend.entity.ContentPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 按业务 key 取内容页，供 {@link com.myweb.backend.service.ContentQueryService} 使用。 */
public interface ContentPageRepository extends JpaRepository<ContentPageEntity, Long> {
    Optional<ContentPageEntity> findByPageKey(String pageKey);
}

