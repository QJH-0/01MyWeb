package com.myweb.backend.repository;

import com.myweb.backend.entity.BlogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** 博客查询：公开列表用 EXISTS 避免标签关联导致分页重复；管理端不按标签筛。 */
public interface BlogRepository extends JpaRepository<BlogEntity, Long> {
    @Query("""
            SELECT b FROM BlogEntity b WHERE b.deletedAt IS NULL AND b.status = 'PUBLISHED'
            AND (:category IS NULL OR b.category = :category)
            AND (:tag IS NULL OR EXISTS (
                SELECT 1 FROM BlogTagEntity t WHERE t.blogId = b.id AND t.tag = :tag))
            """)
    Page<BlogEntity> findPublished(
            @Param("category") String category,
            @Param("tag") String tag,
            Pageable pageable
    );

    @Query("""
            SELECT b FROM BlogEntity b WHERE b.deletedAt IS NULL
            AND (:status IS NULL OR b.status = :status)
            AND (:category IS NULL OR b.category = :category)
            """)
    Page<BlogEntity> findAdminVisible(
            @Param("status") String status,
            @Param("category") String category,
            Pageable pageable
    );

    Optional<BlogEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<BlogEntity> findByIdAndDeletedAtIsNullAndStatus(Long id, String status);

    Optional<BlogEntity> findBySlugAndDeletedAtIsNullAndStatus(String slug, String status);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNullAndIdNot(String slug, Long id);
}
