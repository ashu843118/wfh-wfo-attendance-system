package com.wfhwfo.attendance.outbox.factory;

import com.wfhwfo.attendance.outbox.processor.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventProcessorFactory {

    private final List<OutboxEventProcessor> processors;

    public List<OutboxEventProcessor> getProcessors(String eventType) {
        return processors.stream()
                .filter(processor -> processor.supports(eventType))
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
    }
}
