package com.wfhwfo.attendance.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.audit.service.AuditService;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Order(99)
@RequiredArgsConstructor
public class AuditProcessor implements OutboxEventProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            OutboxEventType.ATTENDANCE_CHECKED_IN.name(),
            OutboxEventType.ATTENDANCE_CHECKED_OUT.name(),
            OutboxEventType.ATTENDANCE_CLASSIFICATION_COMPLETED.name()
    );

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    @Transactional
    public void process(OutboxEvent event) throws Exception {
        OutboxEventPayload payload = objectMapper.readValue(event.getPayload(), OutboxEventPayload.class);
        Long actorId = payload.getEmployeeId();
        auditService.log(
                actorId,
                "OUTBOX_" + event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                "Processed outbox event " + event.getId());
    }
}
