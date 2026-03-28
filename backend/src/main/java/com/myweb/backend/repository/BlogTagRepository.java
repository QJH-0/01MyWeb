package com.myweb.backend.repository;

import com.myweb.backend.entity.BlogTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/** 博客标签批量替换：列表 N+1 由 {@code findAllByBlogIdIn} 缓解。 */
public interface BlogTagRepository extends JpaRepository<BlogTagEntity, Long> {
    List<BlogTagEntity> findAllByBlogIdIn(Collection<Long> blogIds);

    List<BlogTagEntity> findAllByBlogId(Long blogId);

    @Transactional
    void deleteAllByBlogId(Long blogId);
}
