package com.wfhwfo.attendance.outlier.rule;

import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;

import java.util.Optional;

public interface OutlierRule {

    OutlierType getType();

    Optional<OutlierDetectionResult> evaluate(OutlierContext context);

    record OutlierContext(Long employeeId, Long teamId, Long attendanceRecordId) {
    }

    record OutlierDetectionResult(OutlierType outlierType, Severity severity, String description) {
    }
}
