package com.wfhwfo.attendance.outlier.service;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import com.wfhwfo.attendance.outlier.rule.OutlierRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutlierDetectionService {

    private final List<OutlierRule> outlierRules;
    private final AttendanceOutlierRepository attendanceOutlierRepository;

    @Transactional
    public void detectForEmployee(Long employeeId, Long teamId, Long attendanceRecordId) {
        OutlierRule.OutlierContext context = new OutlierRule.OutlierContext(employeeId, teamId, attendanceRecordId);

        for (OutlierRule rule : outlierRules) {
            rule.evaluate(context).ifPresent(result -> saveIfAbsent(
                    employeeId,
                    teamId,
                    attendanceRecordId,
                    result
            ));
        }
    }

    private void saveIfAbsent(Long employeeId, Long teamId, Long attendanceRecordId,
                              OutlierRule.OutlierDetectionResult result) {
        if (attendanceOutlierRepository.existsByEmployeeIdAndOutlierTypeAndStatus(
                employeeId, result.outlierType(), OutlierStatus.OPEN)) {
            return;
        }

        AttendanceOutlier outlier = AttendanceOutlier.builder()
                .employeeId(employeeId)
                .teamId(teamId)
                .attendanceRecordId(attendanceRecordId)
                .outlierType(result.outlierType())
                .severity(result.severity())
                .description(result.description())
                .status(OutlierStatus.OPEN)
                .detectedAt(LocalDateTime.now())
                .build();
        attendanceOutlierRepository.save(outlier);
    }
}
