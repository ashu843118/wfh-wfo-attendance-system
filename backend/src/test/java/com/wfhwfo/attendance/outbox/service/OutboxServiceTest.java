package com.wfhwfo.attendance.outbox.service;

import com.wfhwfo.attendance.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void markProcessedUsesConditionalUpdate() {
        when(outboxEventRepository.markProcessedIfProcessing(10L)).thenReturn(1);

        assertThat(outboxService.markProcessed(10L)).isTrue();
        verify(outboxEventRepository).markProcessedIfProcessing(10L);
    }

    @Test
    void markFailedUsesConditionalUpdate() {
        when(outboxEventRepository.markFailedIfProcessing(11L, "error")).thenReturn(1);

        assertThat(outboxService.markFailed(11L, "error")).isTrue();
        verify(outboxEventRepository).markFailedIfProcessing(11L, "error");
    }

    @Test
    void resetStaleProcessingEvents() {
        when(outboxEventRepository.resetStaleProcessingEvents()).thenReturn(2);

        assertThat(outboxService.resetStaleProcessingEvents()).isEqualTo(2);
    }
}
