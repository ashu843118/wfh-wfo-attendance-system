package com.wfhwfo.attendance.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.dto.PagedResponseMapper;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardDrilldownDto;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardDrilldownType;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardResponse;
import com.wfhwfo.attendance.dashboard.dto.TeamAttendanceRowResponse;
import com.wfhwfo.attendance.dashboard.repository.ManagerDashboardDrilldownRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardService {

    private static final List<AttendanceStatus> PRESENT_STATUSES =
            List.of(AttendanceStatus.CHECKED_IN, AttendanceStatus.CHECKED_OUT,
                    AttendanceStatus.SYSTEM_CLOSED, AttendanceStatus.MISSING_CHECKOUT);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceOutlierRepository attendanceOutlierRepository;
    private final ManagerDashboardDrilldownRepository drilldownRepository;
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
    public PagedResponse<TeamAttendanceRowResponse> getTeamAttendanceRows(LocalDate date, Pageable pageable) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        Page<Object[]> page = attendanceRecordRepository.findTeamDashboardRowsPage(managerId, targetDate, pageable);
        return PagedResponseMapper.from(page, this::toTeamAttendanceRow);
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

    @Transactional(readOnly = true)
    public PagedResponse<ManagerDashboardDrilldownDto> getDrilldown(
            ManagerDashboardDrilldownType type,
            LocalDate date,
            Pageable pageable) {
        Long managerId = SecurityUtils.currentUser().getEmployeeId();
        LocalDate targetDate = date != null ? date : LocalDate.now();

        log.info(
                "Manager drilldown requested managerId={} type={} date={} page={} size={}",
                managerId,
                type,
                targetDate,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<Object[]> page = switch (type) {
            case TEAM_SIZE -> drilldownRepository.findTeamSizeDrilldown(managerId, targetDate, pageable);
            case PRESENT -> drilldownRepository.findPresentDrilldown(
                    managerId, targetDate, PRESENT_STATUSES, pageable);
            case WFO -> drilldownRepository.findModeDrilldown(
                    managerId, targetDate, AttendanceMode.WFO, pageable);
            case WFH -> drilldownRepository.findModeDrilldown(
                    managerId, targetDate, AttendanceMode.WFH, pageable);
            case ABSENT -> drilldownRepository.findAbsentDrilldown(
                    managerId, targetDate, PRESENT_STATUSES, pageable);
            case OUTLIERS -> drilldownRepository.findOutlierDrilldown(
                    managerId, OutlierStatus.OPEN, pageable);
        };

        PagedResponse<ManagerDashboardDrilldownDto> response = type == ManagerDashboardDrilldownType.OUTLIERS
                ? PagedResponseMapper.from(page, this::toOutlierDrilldown)
                : PagedResponseMapper.from(page, this::toEmployeeDrilldown);

        log.info(
                "Manager drilldown result managerId={} type={} date={} page={} size={} totalElements={}",
                managerId,
                type,
                targetDate,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                response.getTotalElements());

        return response;
    }

    private ManagerDashboardDrilldownDto toEmployeeDrilldown(Object[] row) {
        AttendanceStatus status = row[4] != null ? (AttendanceStatus) row[4] : null;
        AttendanceMode mode = row[5] != null ? (AttendanceMode) row[5] : null;
        java.time.LocalDateTime checkIn = row[6] != null ? (java.time.LocalDateTime) row[6] : null;
        java.time.LocalDateTime checkOut = row[7] != null ? (java.time.LocalDateTime) row[7] : null;
        Integer officeMinutes = row[8] != null ? (Integer) row[8] : null;
        com.wfhwfo.attendance.common.enums.CurrentSessionStatus sessionStatus =
                row[9] != null ? (com.wfhwfo.attendance.common.enums.CurrentSessionStatus) row[9] : null;
        Long outlierCount = row[10] != null ? (Long) row[10] : 0L;

        return ManagerDashboardDrilldownDto.builder()
                .employeeId((Long) row[0])
                .employeeName((String) row[1])
                .email((String) row[2])
                .assignedOfficeName(row[3] != null ? (String) row[3] : null)
                .todayStatus(status != null ? status.name() : "ABSENT")
                .attendanceMode(mode != null ? mode.name() : null)
                .firstCheckInTime(checkIn)
                .finalCheckOutTime(checkOut)
                .totalOfficeMinutes(officeMinutes)
                .currentSessionStatus(sessionStatus != null ? sessionStatus.name() : null)
                .outlierCount(outlierCount)
                .build();
    }

    private ManagerDashboardDrilldownDto toOutlierDrilldown(Object[] row) {
        return ManagerDashboardDrilldownDto.builder()
                .outlierId((Long) row[0])
                .employeeId((Long) row[1])
                .employeeName((String) row[2])
                .outlierType((com.wfhwfo.attendance.common.enums.OutlierType) row[3])
                .severity((com.wfhwfo.attendance.common.enums.Severity) row[4])
                .description((String) row[5])
                .detectedAt(row[6] != null ? (java.time.LocalDateTime) row[6] : null)
                .outlierStatus((OutlierStatus) row[7])
                .build();
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
                    .checkInTime(checkIn)
                    .checkOutTime(checkOut)
                    .build());
        }
        return tableRows;
    }

    private TeamAttendanceRowResponse toTeamAttendanceRow(Object[] row) {
        AttendanceStatus status = row[2] != null ? (AttendanceStatus) row[2] : null;
        AttendanceMode mode = row[3] != null ? (AttendanceMode) row[3] : null;
        Boolean late = row[4] != null ? (Boolean) row[4] : null;
        java.time.LocalDateTime checkIn = row[5] != null ? (java.time.LocalDateTime) row[5] : null;
        java.time.LocalDateTime checkOut = row[6] != null ? (java.time.LocalDateTime) row[6] : null;

        return TeamAttendanceRowResponse.builder()
                .employeeId((Long) row[0])
                .employeeName((String) row[1])
                .status(status != null ? status.name() : "ABSENT")
                .mode(mode != null ? mode.name() : null)
                .late(late)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .build();
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
