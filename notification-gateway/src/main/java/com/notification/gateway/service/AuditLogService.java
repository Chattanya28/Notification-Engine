package com.notification.gateway.service;

import com.notification.gateway.model.AuditLog;
import com.notification.gateway.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Logs an action asynchronously to avoid blocking the caller thread.
     */
    @Async
    @Transactional
    public void logAsync(String apiKeyName, String action, String ipAddress, String details) {
        log.debug("AuditLog: {} | Key: {} | IP: {} | Details: {}", action, apiKeyName, ipAddress, details);
        AuditLog auditLog = AuditLog.builder()
                .apiKeyName(apiKeyName)
                .action(action)
                .ipAddress(ipAddress)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
