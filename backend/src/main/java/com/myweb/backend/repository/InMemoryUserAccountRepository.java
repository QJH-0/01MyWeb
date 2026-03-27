package com.myweb.backend.repository;

import com.myweb.backend.entity.UserAccount;
import com.myweb.backend.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryUserAccountRepository implements UserAccountRepository {
    private final AtomicLong sequence = new AtomicLong(1L);
    private final ConcurrentMap<String, UserAccount> byUsername = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, UserAccount> byId = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(byUsername.get(username));
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return Optional.ofNullable(byId.get(userId));
    }

    @Override
    public UserAccount save(String username, String passwordHash) {
        long userId = sequence.getAndIncrement();
        UserAccount account = new UserAccount(userId, username, passwordHash, Set.of("ROLE_USER"));
        UserAccount existing = byUsername.putIfAbsent(username, account);
        if (existing != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "username already exists");
        }
        byId.put(account.userId(), account);
        return account;
    }

    @Override
    public UserAccount saveAdminIfAbsent(String username, String passwordHash) {
        return saveWithRoles(username, passwordHash, Set.of("ROLE_ADMIN", "ROLE_USER"));
    }

    private UserAccount saveWithRoles(String username, String passwordHash, Set<String> roles) {
        long userId = sequence.getAndIncrement();
        UserAccount account = new UserAccount(userId, username, passwordHash, roles);
        UserAccount existing = byUsername.putIfAbsent(username, account);
        if (existing != null) {
            return existing;
        }
        byId.put(account.userId(), account);
        return account;
    }
}
