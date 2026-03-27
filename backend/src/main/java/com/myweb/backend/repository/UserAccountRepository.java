package com.myweb.backend.repository;

import com.myweb.backend.entity.UserAccount;

import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(long userId);

    UserAccount save(String username, String passwordHash);

    UserAccount saveAdminIfAbsent(String username, String passwordHash);
}
