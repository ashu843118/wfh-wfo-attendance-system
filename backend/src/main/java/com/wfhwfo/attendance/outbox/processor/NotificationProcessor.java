package com.wfhwfo.attendance.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.notification.service.NotificationService;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(4)
@RequiredArgsConstructor
public class NotificationProcessor implements OutboxEventProcessor {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return OutboxEventType.ATTENDANCE_CLASSIFICATION_COMPLETED.name().equals(eventType);
    }

    @Override
    @Transactional
    public void process(OutboxEvent event) throws Exception {
        OutboxEventPayload payload = objectMapper.readValue(event.getPayload(), OutboxEventPayload.class);
        notificationService.notifyForOutliers(payload.getEmployeeId(), payload.getManagerId());
    }
}
