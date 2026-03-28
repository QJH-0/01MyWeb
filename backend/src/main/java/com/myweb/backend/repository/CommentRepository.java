package com.myweb.backend.repository;

import com.myweb.backend.entity.CommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 按目标分页列出未软删评论，顺序由 {@link Pageable} 的 Sort 决定。 */
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    Page<CommentEntity> findByTargetTypeAndTargetIdAndDeletedAtIsNull(String targetType, Long targetId, Pageable pageable);
}
