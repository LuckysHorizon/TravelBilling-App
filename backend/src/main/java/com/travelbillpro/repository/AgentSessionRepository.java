package com.travelbillpro.repository;

import com.travelbillpro.entity.AgentSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {
    Page<AgentSession> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    Optional<AgentSession> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
}
