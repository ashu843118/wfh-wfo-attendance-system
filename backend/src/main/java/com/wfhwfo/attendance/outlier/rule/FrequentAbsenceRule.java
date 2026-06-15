package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FrequentAbsenceRule implements OutlierRule {

    private static final int ABSENCE_THRESHOLD = 3;
    private static final int WORKING_DAYS_WINDOW = 7;

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    public OutlierType getType() {
        return OutlierType.FREQUENT_ABSENCE;
    }

    @Override
    public Optional<OutlierDetectionResult> evaluate(OutlierContext context) {
        LocalDate to = LocalDate.now();
        LocalDate from = FrequentLateCheckInRule.startOfWorkingDaysWindow(to, WORKING_DAYS_WINDOW).toLocalDate();
        long presentDays = attendanceRecordRepository.countPresentDays(context.employeeId(), from, to);
        long workingDays = countWorkingDays(from, to);
        long absentDays = workingDays - presentDays;

        if (absentDays > ABSENCE_THRESHOLD) {
            return Optional.of(new OutlierDetectionResult(
                    OutlierType.FREQUENT_ABSENCE,
                    Severity.CRITICAL,
                    "Employee was absent " + absentDays + " of the last " + workingDays + " working days"
            ));
        }
        return Optional.empty();
    }

    private long countWorkingDays(LocalDate from, LocalDate to) {
        long count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }
}
