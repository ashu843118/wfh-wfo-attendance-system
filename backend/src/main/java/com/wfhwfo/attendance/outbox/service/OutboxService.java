package com.wfhwfo.attendance.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.common.enums.OutboxStatus;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import com.wfhwfo.attendance.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    @Transactional
    public OutboxEvent saveEvent(OutboxEventType eventType, String aggregateType, Long aggregateId, Object payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType.name())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(serializePayload(payload))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(maxRetries)
                .build();
        return outboxEventRepository.save(event);
    }

    @Transactional
    public boolean markProcessed(Long eventId) {
        return outboxEventRepository.markProcessedIfProcessing(eventId) == 1;
    }

    @Transactional
    public boolean markFailed(Long eventId, String errorMessage) {
        return outboxEventRepository.markFailedIfProcessing(eventId, truncate(errorMessage)) == 1;
    }

    @Transactional
    public int resetStaleProcessingEvents() {
        return outboxEventRepository.resetStaleProcessingEvents();
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
