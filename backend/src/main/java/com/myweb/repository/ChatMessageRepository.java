package com.myweb.repository;

import com.myweb.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20BySessionIdOrderByIdDesc(String sessionId);
}
