package com.wfhwfo.attendance.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(3)
@RequiredArgsConstructor
public class DashboardCacheRefreshProcessor implements OutboxEventProcessor {

    private final CacheAdapter cacheAdapter;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return OutboxEventType.ATTENDANCE_CLASSIFICATION_COMPLETED.name().equals(eventType);
    }

    @Override
    public void process(OutboxEvent event) throws Exception {
        OutboxEventPayload payload = objectMapper.readValue(event.getPayload(), OutboxEventPayload.class);
        LocalDate today = LocalDate.now();
        String dateKey = today.toString();

        if (payload.getManagerId() != null) {
            cacheAdapter.evict("manager:dashboard:" + payload.getManagerId() + ":" + dateKey);
        }

        cacheAdapter.evict("leadership:dashboard:" + dateKey);
    }
}
