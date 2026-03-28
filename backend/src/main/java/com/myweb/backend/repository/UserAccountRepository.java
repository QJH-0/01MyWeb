package com.myweb.backend.repository;

import com.myweb.backend.entity.UserAccountEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 账户按用户名唯一查找，用于登录与注册冲突检测。 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findByUsername(String username);
}
