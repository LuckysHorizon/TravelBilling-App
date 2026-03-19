package com.travelbillpro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbillpro.entity.AuditLog;
import com.travelbillpro.entity.User;
import com.travelbillpro.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, Long entityId, String action, Object oldValue, Object newValue, User user) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setAction(action);
            
            if (oldValue != null) {
                auditLog.setOldValue(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValue(objectMapper.writeValueAsString(newValue));
            }
            
            auditLog.setUser(user);
            auditLog.setIpAddress(getClientIp());
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to write audit log for {} id {} action {}", entityType, entityId, action, e);
            // We intentionally do not throw here for non-financial audit logs to prevent breaking main flow
            // Note: For strict financial compliance, we might want to throw and rollback the main transaction
        }
    }

    private String getClientIp() {
        try {
            String remoteAddr = "";
            if (request != null) {
                remoteAddr = request.getHeader("X-FORWARDED-FOR");
                if (remoteAddr == null || "".equals(remoteAddr)) {
                    remoteAddr = request.getRemoteAddr();
                }
            }
            return remoteAddr;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
