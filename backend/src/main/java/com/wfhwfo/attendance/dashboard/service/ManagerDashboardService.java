package com.wfhwfo.attendance.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardResponse;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<AttendanceStatus> PRESENT_STATUSES =
            List.of(AttendanceStatus.CHECKED_IN, AttendanceStatus.CHECKED_OUT,
                    AttendanceStatus.SYSTEM_CLOSED, AttendanceStatus.MISSING_CHECKOUT);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceOutlierRepository attendanceOutlierRepository;
    private final CacheAdapter cacheAdapter;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.dashboard-ttl-seconds:60}")
    private long dashboardTtlSeconds;

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboardSummary(LocalDate date) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String cacheKey = "manager:dashboard:" + managerId + ":" + targetDate;

        return cacheAdapter.get(cacheKey)
                .flatMap(json -> deserialize(json, ManagerDashboardResponse.class))
                .orElseGet(() -> loadAndCacheDashboard(managerId, targetDate, cacheKey));
    }

    @Transactional(readOnly = true)
    public Page<AttendanceRecord> getTeamAttendance(LocalDate date, Pageable pageable) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return attendanceRecordRepository.findTeamAttendanceByManagerAndDate(managerId, targetDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<com.wfhwfo.attendance.outlier.entity.AttendanceOutlier> getOutliers(Pageable pageable) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        return attendanceOutlierRepository.findByManagerIdAndStatus(managerId, OutlierStatus.OPEN, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceRecord> getEmployeeAttendance(Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate fromDate = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? to : LocalDate.now();
        return attendanceRecordRepository.findEmployeeAttendanceForManager(
                managerId, employeeId, fromDate, toDate, pageable);
    }

    private ManagerDashboardResponse loadAndCacheDashboard(Long managerId, LocalDate date, String cacheKey) {
        long teamSize = employeeRepository.countByManagerIdAndActiveTrue(managerId);
        long presentToday = attendanceRecordRepository.countByManagerAndDateAndStatusIn(managerId, date, PRESENT_STATUSES);
        long wfoToday = attendanceRecordRepository.countByManagerDateAndMode(managerId, date, AttendanceMode.WFO);
        long wfhToday = attendanceRecordRepository.countByManagerDateAndMode(managerId, date, AttendanceMode.WFH);
        long lateToday = attendanceRecordRepository.countLateByManagerAndDate(managerId, date);
        long pendingClassification = attendanceRecordRepository.countByManagerDateAndProcessingStatus(
                managerId, date, ProcessingStatus.CLASSIFICATION_PENDING);
        long openOutliers = attendanceOutlierRepository.countByManagerIdAndStatus(managerId, OutlierStatus.OPEN);
        long absentToday = Math.max(0L, teamSize - presentToday);

        ManagerDashboardResponse.KpiSummary kpis = ManagerDashboardResponse.KpiSummary.builder()
                .teamSize(teamSize)
                .presentToday(presentToday)
                .absentToday(absentToday)
                .wfoToday(wfoToday)
                .wfhToday(wfhToday)
                .lateToday(lateToday)
                .pendingClassification(pendingClassification)
                .openOutliers(openOutliers)
                .build();

        List<ManagerDashboardResponse.ChartSlice> pieChart = buildModePieChart(managerId, date);
        List<ManagerDashboardResponse.BarChartPoint> barChart = buildMonthlyBarChart(managerId, date);
        List<ManagerDashboardResponse.TeamAttendanceRow> tableRows = buildTeamTable(managerId, date);

        ManagerDashboardResponse response = ManagerDashboardResponse.builder()
                .kpis(kpis)
                .modePieChart(pieChart)
                .monthlyBarChart(barChart)
                .teamTable(tableRows)
                .build();

        cacheResponse(cacheKey, response);
        return response;
    }

    private List<ManagerDashboardResponse.ChartSlice> buildModePieChart(Long managerId, LocalDate date) {
        List<Object[]> rows = attendanceRecordRepository.countModeSplitByManagerAndDate(managerId, date);
        List<ManagerDashboardResponse.ChartSlice> slices = new ArrayList<>();
        for (Object[] row : rows) {
            AttendanceMode mode = (AttendanceMode) row[0];
            long count = (Long) row[1];
            slices.add(ManagerDashboardResponse.ChartSlice.builder()
                    .label(mode != null ? mode.name() : "UNKNOWN")
                    .value(count)
                    .build());
        }
        return slices;
    }

    private List<ManagerDashboardResponse.BarChartPoint> buildMonthlyBarChart(Long managerId, LocalDate date) {
        LocalDate from = date.withDayOfMonth(1);
        List<Object[]> rows = attendanceRecordRepository.monthlySummaryByManager(managerId, from, date);
        List<ManagerDashboardResponse.BarChartPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            points.add(ManagerDashboardResponse.BarChartPoint.builder()
                    .employeeName((String) row[0])
                    .presentDays((Long) row[1])
                    .wfoDays((Long) row[2])
                    .wfhDays((Long) row[3])
                    .build());
        }
        return points;
    }

    private List<ManagerDashboardResponse.TeamAttendanceRow> buildTeamTable(Long managerId, LocalDate date) {
        List<Object[]> rows = attendanceRecordRepository.findTeamDashboardRows(managerId, date);
        List<ManagerDashboardResponse.TeamAttendanceRow> tableRows = new ArrayList<>();
        for (Object[] row : rows) {
            AttendanceStatus status = row[2] != null ? (AttendanceStatus) row[2] : null;
            AttendanceMode mode = row[3] != null ? (AttendanceMode) row[3] : null;
            Boolean late = row[4] != null ? (Boolean) row[4] : null;
            java.time.LocalDateTime checkIn = row[5] != null ? (java.time.LocalDateTime) row[5] : null;
            java.time.LocalDateTime checkOut = row[6] != null ? (java.time.LocalDateTime) row[6] : null;

            tableRows.add(ManagerDashboardResponse.TeamAttendanceRow.builder()
                    .employeeId((Long) row[0])
                    .employeeName((String) row[1])
                    .status(status != null ? status.name() : "ABSENT")
                    .mode(mode != null ? mode.name() : null)
                    .late(late)
                    .checkInTime(checkIn != null ? checkIn.format(TIME_FORMAT) : null)
                    .checkOutTime(checkOut != null ? checkOut.format(TIME_FORMAT) : null)
                    .build());
        }
        return tableRows;
    }

    private void cacheResponse(String cacheKey, ManagerDashboardResponse response) {
        try {
            cacheAdapter.put(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(dashboardTtlSeconds));
        } catch (Exception ex) {
            log.warn("Failed to cache manager dashboard for key {}", cacheKey, ex);
        }
    }

    private <T> Optional<T> deserialize(String json, Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
