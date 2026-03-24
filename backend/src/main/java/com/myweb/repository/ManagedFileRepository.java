package com.myweb.repository;

import com.myweb.entity.ManagedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagedFileRepository extends JpaRepository<ManagedFile, Long> {
}
