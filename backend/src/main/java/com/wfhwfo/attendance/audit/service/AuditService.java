package com.wfhwfo.attendance.audit.service;

import com.wfhwfo.attendance.audit.entity.AuditLog;
import com.wfhwfo.attendance.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(Long actorEmployeeId, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .actorEmployeeId(actorEmployeeId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
