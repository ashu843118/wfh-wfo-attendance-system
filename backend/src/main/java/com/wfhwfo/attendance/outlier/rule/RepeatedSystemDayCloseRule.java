package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RepeatedSystemDayCloseRule implements OutlierRule {

    private static final int WINDOW_DAYS = 30;
    private static final int THRESHOLD = 3;

    private final AttendanceEventRepository attendanceEventRepository;

    @Override
    public OutlierType getType() {
        return OutlierType.REPEATED_SYSTEM_DAY_CLOSE;
    }

    @Override
    public Optional<OutlierDetectionResult> evaluate(OutlierContext context) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(WINDOW_DAYS);
        long systemCloseCount = attendanceEventRepository.countByEmployeeIdAndEventTypeAndAttendanceDateBetween(
                context.employeeId(), AttendanceEventType.SYSTEM_DAY_CLOSE, from, to);

        if (systemCloseCount >= THRESHOLD) {
            return Optional.of(new OutlierDetectionResult(
                    OutlierType.REPEATED_SYSTEM_DAY_CLOSE,
                    Severity.WARNING,
                    "Employee had " + systemCloseCount + " system day-close events in the last " + WINDOW_DAYS + " days"
            ));
        }
        return Optional.empty();
    }
}
