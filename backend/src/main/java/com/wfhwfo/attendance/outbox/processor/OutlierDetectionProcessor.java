package com.wfhwfo.attendance.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import com.wfhwfo.attendance.outlier.service.OutlierDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@RequiredArgsConstructor
public class OutlierDetectionProcessor implements OutboxEventProcessor {

    private final OutlierDetectionService outlierDetectionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return OutboxEventType.ATTENDANCE_CLASSIFICATION_COMPLETED.name().equals(eventType);
    }

    @Override
    @Transactional
    public void process(OutboxEvent event) throws Exception {
        OutboxEventPayload payload = objectMapper.readValue(event.getPayload(), OutboxEventPayload.class);
        outlierDetectionService.detectForEmployee(
                payload.getEmployeeId(),
                payload.getTeamId(),
                payload.getAttendanceRecordId()
        );
    }
}
