package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LowWfoAttendanceRule implements OutlierRule {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;

    @Override
    public OutlierType getType() {
        return OutlierType.LOW_WFO_ATTENDANCE;
    }

    @Override
    public Optional<OutlierDetectionResult> evaluate(OutlierContext context) {
        if (context.teamId() == null) {
            return Optional.empty();
        }

        AttendancePolicy policy = attendancePolicyRepository.findByTeamIdAndActiveTrue(context.teamId()).orElse(null);
        if (policy == null) {
            return Optional.empty();
        }

        LocalDate weekStart = startOfWeek(LocalDate.now());
        LocalDate weekEnd = LocalDate.now();
        long wfoDays = attendanceRecordRepository.countModeDays(
                context.employeeId(), weekStart, weekEnd, AttendanceMode.WFO);

        if (wfoDays < policy.getMinimumWfoDaysPerWeek()) {
            return Optional.of(new OutlierDetectionResult(
                    OutlierType.LOW_WFO_ATTENDANCE,
                    Severity.WARNING,
                    "Employee has " + wfoDays + " WFO days this week; minimum required is "
                            + policy.getMinimumWfoDaysPerWeek()
            ));
        }
        return Optional.empty();
    }

    private LocalDate startOfWeek(LocalDate date) {
        LocalDate cursor = date;
        while (cursor.getDayOfWeek() != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1);
        }
        return cursor;
    }
}
