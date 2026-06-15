package com.wfhwfo.attendance.dashboard.service;

import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.dashboard.dto.EmployeeDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeDashboardService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int TREND_DAYS = 30;

    private final AttendanceRecordRepository attendanceRecordRepository;

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboard() {
        Long employeeId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate trendStart = today.minusDays(TREND_DAYS - 1L);

        long presentDays = attendanceRecordRepository.countPresentDays(employeeId, monthStart, today);
        long wfoDays = attendanceRecordRepository.countModeDays(employeeId, monthStart, today, AttendanceMode.WFO);
        long wfhDays = attendanceRecordRepository.countModeDays(employeeId, monthStart, today, AttendanceMode.WFH);
        long lateDays = attendanceRecordRepository.countLateDays(employeeId, monthStart, today);
        long pendingClassification = attendanceRecordRepository.countProcessingStatus(
                employeeId, monthStart, today, ProcessingStatus.CLASSIFICATION_PENDING);
        long missingCheckoutDays = attendanceRecordRepository.countMissingCheckoutDays(
                employeeId, monthStart, today);

        EmployeeDashboardResponse.EmployeeKpis kpis = EmployeeDashboardResponse.EmployeeKpis.builder()
                .presentDaysThisMonth(presentDays)
                .wfoDaysThisMonth(wfoDays)
                .wfhDaysThisMonth(wfhDays)
                .lateDaysThisMonth(lateDays)
                .pendingClassification(pendingClassification)
                .missingCheckoutDaysThisMonth(missingCheckoutDays)
                .build();

        List<EmployeeDashboardResponse.TrendPoint> trend = buildTrend(employeeId, trendStart, today);

        List<EmployeeDashboardResponse.RecentAttendanceRow> recent = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDateBetween(employeeId, monthStart, today, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(this::toRecentRow)
                .toList();

        return EmployeeDashboardResponse.builder()
                .kpis(kpis)
                .wfoWfhTrend(trend)
                .recentAttendance(recent)
                .build();
    }

    private List<EmployeeDashboardResponse.TrendPoint> buildTrend(
            Long employeeId, LocalDate from, LocalDate to) {
        Map<LocalDate, long[]> countsByDate = new HashMap<>();
        for (Object[] row : attendanceRecordRepository.employeeWfoWfhTrend(employeeId, from, to)) {
            LocalDate date = (LocalDate) row[0];
            AttendanceMode mode = (AttendanceMode) row[1];
            ProcessingStatus processingStatus = (ProcessingStatus) row[2];
            if (processingStatus != ProcessingStatus.COMPLETED || mode == null) {
                continue;
            }
            long[] counts = countsByDate.computeIfAbsent(date, ignored -> new long[]{0L, 0L});
            if (mode == AttendanceMode.WFO) {
                counts[0] = 1;
            } else if (mode == AttendanceMode.WFH) {
                counts[1] = 1;
            }
        }

        List<EmployeeDashboardResponse.TrendPoint> trend = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            long[] counts = countsByDate.getOrDefault(date, new long[]{0L, 0L});
            trend.add(EmployeeDashboardResponse.TrendPoint.builder()
                    .date(date.format(DATE_FORMAT))
                    .wfoCount(counts[0])
                    .wfhCount(counts[1])
                    .build());
        }
        return trend;
    }

    private EmployeeDashboardResponse.RecentAttendanceRow toRecentRow(AttendanceRecord record) {
        return EmployeeDashboardResponse.RecentAttendanceRow.builder()
                .date(record.getAttendanceDate().format(DATE_FORMAT))
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .mode(record.getAttendanceMode() != null ? record.getAttendanceMode().name() : null)
                .late(record.getLate())
                .checkInTime(record.getFirstCheckInTime() != null ? record.getFirstCheckInTime().format(TIME_FORMAT) : null)
                .checkOutTime(record.getFinalCheckOutTime() != null ? record.getFinalCheckOutTime().format(TIME_FORMAT) : null)
                .build();
    }
}
