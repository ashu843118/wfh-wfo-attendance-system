package com.wfhwfo.attendance.outbox.scheduler;

import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import com.wfhwfo.attendance.outbox.factory.OutboxEventProcessorFactory;
import com.wfhwfo.attendance.outbox.processor.OutboxEventProcessor;
import com.wfhwfo.attendance.outbox.repository.OutboxEventRepository;
import com.wfhwfo.attendance.outbox.service.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessorFactory processorFactory;
    private final OutboxService outboxService;
    private final Executor outboxTaskExecutor;

    @Value("${app.outbox.batch-size:10}")
    private int batchSize;

    public OutboxPoller(
            OutboxEventRepository outboxEventRepository,
            OutboxEventProcessorFactory processorFactory,
            OutboxService outboxService,
            @Qualifier("outboxTaskExecutor") Executor outboxTaskExecutor) {
        this.outboxEventRepository = outboxEventRepository;
        this.processorFactory = processorFactory;
        this.outboxService = outboxService;
        this.outboxTaskExecutor = outboxTaskExecutor;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void poll() {
        outboxService.resetStaleProcessingEvents();

        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(batchSize);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Outbox polling started pendingEvents={}", pendingEvents.size());
        int claimedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            int claimed = outboxEventRepository.claimEvent(event.getId());
            if (claimed == 1) {
                claimedCount++;
                log.info("Outbox event claimed eventId={} eventType={}", event.getId(), event.getEventType());
                outboxTaskExecutor.execute(() -> processEvent(event.getId()));
            }
        }

        if (claimedCount > 0) {
            log.debug("Outbox polling completed claimedEvents={}", claimedCount);
        }
    }

    private void processEvent(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }

        List<OutboxEventProcessor> processors = processorFactory.getProcessors(event.getEventType());
        try {
            for (OutboxEventProcessor processor : processors) {
                processor.process(event);
            }
            if (!outboxService.markProcessed(eventId)) {
                log.warn("Outbox event {} was not in PROCESSING state when marking processed", eventId);
            } else {
                log.info("Outbox event processed eventId={} eventType={}", eventId, event.getEventType());
            }
        } catch (Exception ex) {
            log.error("Outbox event failed eventId={} eventType={} reason={}", eventId, event.getEventType(), ex.getMessage(), ex);
            if (!outboxService.markFailed(eventId, ex.getMessage())) {
                log.warn("Outbox event {} was not in PROCESSING state when marking failed", eventId);
            } else {
                log.warn("Outbox event retry scheduled eventId={} retryCount={}", eventId, event.getRetryCount() + 1);
            }
        }
    }
}
