package com.myweb.repository;

import com.myweb.entity.Blog;
import com.myweb.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findAllByStatus(BlogStatus status, Pageable pageable);

    Page<Blog> findAllByStatusAndCategoryContainingIgnoreCase(BlogStatus status, String category, Pageable pageable);

    Page<Blog> findAllByCategoryContainingIgnoreCase(String category, Pageable pageable);
}
