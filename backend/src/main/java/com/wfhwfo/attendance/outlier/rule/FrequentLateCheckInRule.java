package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FrequentLateCheckInRule implements OutlierRule {

    private static final int LATE_THRESHOLD = 3;
    private static final int WORKING_DAYS_WINDOW = 7;

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    public OutlierType getType() {
        return OutlierType.FREQUENT_LATE_CHECK_IN;
    }

    @Override
    public Optional<OutlierDetectionResult> evaluate(OutlierContext context) {
        LocalDateTime since = startOfWorkingDaysWindow(LocalDate.now(), WORKING_DAYS_WINDOW);
        long lateCount = attendanceRecordRepository.countLateSince(context.employeeId(), since);

        if (lateCount > LATE_THRESHOLD) {
            return Optional.of(new OutlierDetectionResult(
                    OutlierType.FREQUENT_LATE_CHECK_IN,
                    Severity.WARNING,
                    "Employee has " + lateCount + " late check-ins in the last "
                            + WORKING_DAYS_WINDOW + " working days"
            ));
        }
        return Optional.empty();
    }

    static LocalDateTime startOfWorkingDaysWindow(LocalDate from, int workingDays) {
        LocalDate cursor = from;
        int counted = 0;
        while (counted < workingDays) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
            if (counted < workingDays) {
                cursor = cursor.minusDays(1);
            }
        }
        return cursor.atStartOfDay();
    }
}
