package com.wfhwfo.attendance.outlier.service;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import com.wfhwfo.attendance.outlier.rule.OutlierRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutlierDetectionService {

    private final List<OutlierRule> outlierRules;
    private final AttendanceOutlierRepository attendanceOutlierRepository;

    @Transactional
    public int detectForEmployee(Long employeeId, Long teamId, Long attendanceRecordId) {
        log.info("Outlier detection started employeeId={} attendanceRecordId={}", employeeId, attendanceRecordId);

        OutlierRule.OutlierContext context = new OutlierRule.OutlierContext(employeeId, teamId, attendanceRecordId);
        int outliersCreated = 0;

        for (OutlierRule rule : outlierRules) {
            if (rule.evaluate(context)
                    .map(result -> saveIfAbsent(employeeId, teamId, attendanceRecordId, result))
                    .orElse(false)) {
                outliersCreated++;
            }
        }

        log.info(
                "Outlier detection completed employeeId={} attendanceRecordId={} outliersCreated={}",
                employeeId,
                attendanceRecordId,
                outliersCreated);
        return outliersCreated;
    }

    private boolean saveIfAbsent(Long employeeId, Long teamId, Long attendanceRecordId,
                                 OutlierRule.OutlierDetectionResult result) {
        if (attendanceOutlierRepository.existsByEmployeeIdAndOutlierTypeAndStatus(
                employeeId, result.outlierType(), OutlierStatus.OPEN)) {
            return false;
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
        return true;
    }
}
