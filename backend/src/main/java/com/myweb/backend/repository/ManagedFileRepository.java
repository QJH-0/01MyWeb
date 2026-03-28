package com.myweb.backend.repository;

import com.myweb.backend.entity.ManagedFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ManagedFileRepository extends JpaRepository<ManagedFileEntity, Long> {

    @Query("""
            SELECT m FROM ManagedFileEntity m
            WHERE m.deletedAt IS NULL
            AND (:fileType IS NULL OR m.fileType = :fileType)
            """)
    Page<ManagedFileEntity> pageActive(@Param("fileType") String fileType, Pageable pageable);

    @Query("""
            SELECT m FROM ManagedFileEntity m WHERE m.id = :id AND m.deletedAt IS NULL
            """)
    Optional<ManagedFileEntity> findActiveById(@Param("id") long id);
}
