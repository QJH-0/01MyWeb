package com.myweb.backend.repository;

import com.myweb.backend.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

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

