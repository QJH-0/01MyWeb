package com.myweb.backend.repository;

import com.myweb.backend.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 项目查询：公开/管理端方法名体现 deletedAt、visible 过滤条件，避免调用方漏过滤。 */
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Page<ProjectEntity> findAllByDeletedAtIsNullAndVisibleTrue(Pageable pageable);

    Page<ProjectEntity> findAllByDeletedAtIsNullAndVisibleTrueAndCategory(String category, Pageable pageable);

    Optional<ProjectEntity> findByIdAndDeletedAtIsNullAndVisibleTrue(Long id);

    Page<ProjectEntity> findAllByDeletedAtIsNull(Pageable pageable);

    Page<ProjectEntity> findAllByDeletedAtIsNullAndCategory(String category, Pageable pageable);

    Page<ProjectEntity> findAllByDeletedAtIsNullAndVisible(boolean visible, Pageable pageable);

    Page<ProjectEntity> findAllByDeletedAtIsNullAndCategoryAndVisible(String category, boolean visible, Pageable pageable);

    Optional<ProjectEntity> findByIdAndDeletedAtIsNull(Long id);
}

