package com.myweb.repository;

import com.myweb.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findAllByVisibleTrue(Pageable pageable);

    Page<Project> findAllByVisibleTrueAndCategoryContainingIgnoreCase(String category, Pageable pageable);

    Page<Project> findAllByCategoryContainingIgnoreCase(String category, Pageable pageable);
}

