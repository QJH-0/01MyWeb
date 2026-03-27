package com.myweb.backend.repository;

import com.myweb.backend.entity.ProjectTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface ProjectTagRepository extends JpaRepository<ProjectTagEntity, Long> {
    List<ProjectTagEntity> findAllByProjectIdIn(Collection<Long> projectIds);

    List<ProjectTagEntity> findAllByProjectId(Long projectId);

    @Transactional
    void deleteAllByProjectId(Long projectId);
}

