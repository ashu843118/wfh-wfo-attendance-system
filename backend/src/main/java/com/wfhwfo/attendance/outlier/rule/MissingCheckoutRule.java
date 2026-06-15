package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MissingCheckoutRule implements OutlierRule {

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    public OutlierType getType() {
        return OutlierType.MISSING_CHECKOUT;
    }

    @Override
    public Optional<OutlierDetectionResult> evaluate(OutlierContext context) {
        List<AttendanceRecord> missingCheckouts = attendanceRecordRepository.findMissingCheckouts(LocalDate.now());
        boolean hasMissingCheckout = missingCheckouts.stream()
                .anyMatch(record -> record.getEmployeeId().equals(context.employeeId()));

        if (hasMissingCheckout) {
            return Optional.of(new OutlierDetectionResult(
                    OutlierType.MISSING_CHECKOUT,
                    Severity.INFO,
                    "Employee checked in today but has not checked out"
            ));
        }
        return Optional.empty();
    }
}
