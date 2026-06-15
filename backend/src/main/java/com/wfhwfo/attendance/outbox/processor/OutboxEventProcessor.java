package com.wfhwfo.attendance.outbox.processor;

import com.wfhwfo.attendance.outbox.entity.OutboxEvent;

public interface OutboxEventProcessor {

    boolean supports(String eventType);

    void process(OutboxEvent event) throws Exception;
}
