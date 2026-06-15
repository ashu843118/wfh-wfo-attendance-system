package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrequentLateCheckInRuleTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @InjectMocks
    private FrequentLateCheckInRule rule;

    @Test
    void detectsFrequentLateCheckIns() {
        when(attendanceRecordRepository.countLateSince(eq(2L), any(LocalDateTime.class))).thenReturn(4L);

        Optional<OutlierRule.OutlierDetectionResult> result = rule.evaluate(
                new OutlierRule.OutlierContext(2L, 1L, 100L));

        assertThat(result).isPresent();
        assertThat(result.get().outlierType()).isEqualTo(OutlierType.FREQUENT_LATE_CHECK_IN);
        assertThat(result.get().severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void ignoresWhenBelowThreshold() {
        when(attendanceRecordRepository.countLateSince(eq(2L), any(LocalDateTime.class))).thenReturn(2L);

        assertThat(rule.evaluate(new OutlierRule.OutlierContext(2L, 1L, 100L))).isEmpty();
    }
}
