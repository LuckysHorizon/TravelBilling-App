package com.travelbillpro.repository;

import com.travelbillpro.entity.AgentMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    Page<AgentMessage> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);
    List<AgentMessage> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);
    long countBySessionId(UUID sessionId);
}
