package com.wfhwfo.attendance.attendance.scheduler;

import com.wfhwfo.attendance.attendance.config.DayCloseProperties;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.attendance.service.AttendanceWriteService;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.notification.service.NotificationService;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import com.wfhwfo.attendance.outlier.service.OutlierDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DayCloseScheduler {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceWriteService attendanceWriteService;
    private final AttendanceOutlierRepository attendanceOutlierRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final OutlierDetectionService outlierDetectionService;
    private final DayCloseProperties dayCloseProperties;

    @Scheduled(cron = "${app.attendance.day-close.cron:0 5 0 * * *}")
    @Transactional
    public void closeOpenAttendanceRecords() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<AttendanceRecord> openRecords = attendanceRecordRepository.findOpenRecordsForDayClose(targetDate);

        if (openRecords.isEmpty()) {
            return;
        }

        log.info("EOD close job started date={} candidateRecords={}", targetDate, openRecords.size());

        LocalDateTime closeTime = targetDate.atTime(parseCloseTime());
        int closedRecords = 0;

        for (AttendanceRecord record : openRecords) {
            if (attendanceEventRepository.existsByEmployeeIdAndAttendanceDateAndEventType(
                    record.getEmployeeId(), targetDate, AttendanceEventType.SYSTEM_DAY_CLOSE)) {
                continue;
            }

            attendanceWriteService.recordSystemDayClose(record, closeTime, "Automatic end-of-day close");
            createMissingCheckoutOutlier(record);
            closedRecords++;

            Employee employee = employeeRepository.findById(record.getEmployeeId()).orElse(null);
            Long managerId = employee != null ? employee.getManagerId() : null;
            notificationService.notifyDayClose(record.getEmployeeId(), managerId, targetDate);
            outlierDetectionService.detectForEmployee(
                    record.getEmployeeId(), record.getTeamId(), record.getId());
        }

        log.info("EOD close completed date={} closedRecords={}", targetDate, closedRecords);
    }

    private void createMissingCheckoutOutlier(AttendanceRecord record) {
        if (attendanceOutlierRepository.existsByEmployeeIdAndOutlierTypeAndStatus(
                record.getEmployeeId(), OutlierType.MISSING_CHECKOUT, OutlierStatus.OPEN)) {
            return;
        }

        AttendanceOutlier outlier = AttendanceOutlier.builder()
                .employeeId(record.getEmployeeId())
                .teamId(record.getTeamId())
                .attendanceRecordId(record.getId())
                .outlierType(OutlierType.MISSING_CHECKOUT)
                .severity(Severity.INFO)
                .description("Employee checked in but no checkout was recorded before day end")
                .status(OutlierStatus.OPEN)
                .detectedAt(LocalDateTime.now())
                .build();
        attendanceOutlierRepository.save(outlier);
    }

    private LocalTime parseCloseTime() {
        return LocalTime.parse(dayCloseProperties.getDefaultCloseTime());
    }
}
