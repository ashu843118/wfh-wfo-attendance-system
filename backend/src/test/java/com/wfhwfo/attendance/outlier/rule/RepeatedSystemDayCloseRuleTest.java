package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.OutlierType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatedSystemDayCloseRuleTest {

    @Mock
    private AttendanceEventRepository attendanceEventRepository;

    @InjectMocks
    private RepeatedSystemDayCloseRule rule;

    @Test
    void detectsRepeatedSystemDayClose() {
        when(attendanceEventRepository.countByEmployeeIdAndEventTypeAndAttendanceDateBetween(
                eq(1L), eq(AttendanceEventType.SYSTEM_DAY_CLOSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3L);

        Optional<OutlierRule.OutlierDetectionResult> result = rule.evaluate(
                new OutlierRule.OutlierContext(1L, 1L, 100L));

        assertThat(result).isPresent();
        assertThat(result.get().outlierType()).isEqualTo(OutlierType.REPEATED_SYSTEM_DAY_CLOSE);
    }

    @Test
    void ignoresBelowThreshold() {
        when(attendanceEventRepository.countByEmployeeIdAndEventTypeAndAttendanceDateBetween(
                eq(1L), eq(AttendanceEventType.SYSTEM_DAY_CLOSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(2L);

        assertThat(rule.evaluate(new OutlierRule.OutlierContext(1L, 1L, 100L))).isEmpty();
    }
}
