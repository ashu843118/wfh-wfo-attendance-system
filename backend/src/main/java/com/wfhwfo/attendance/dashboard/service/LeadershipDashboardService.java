package com.wfhwfo.attendance.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.dashboard.dto.LeadershipDashboardResponse;
import com.wfhwfo.attendance.common.util.WorkingDayUtils;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class LeadershipDashboardService {

    private static final List<AttendanceStatus> PRESENT_STATUSES =
            List.of(AttendanceStatus.CHECKED_IN, AttendanceStatus.CHECKED_OUT,
                    AttendanceStatus.SYSTEM_CLOSED, AttendanceStatus.MISSING_CHECKOUT);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final CacheAdapter cacheAdapter;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.dashboard-ttl-seconds:60}")
    private long dashboardTtlSeconds;

    @Transactional(readOnly = true)
    public LeadershipDashboardResponse getDashboard(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        String cacheKey = "leadership:dashboard:" + targetDate;

        return cacheAdapter.get(cacheKey)
                .flatMap(this::deserialize)
                .orElseGet(() -> loadAndCacheDashboard(targetDate, cacheKey));
    }

    private LeadershipDashboardResponse loadAndCacheDashboard(LocalDate date, String cacheKey) {
        long totalEmployees = employeeRepository.countByActiveTrue();
        long presentToday = attendanceRecordRepository.countByDateAndStatusIn(date, PRESENT_STATUSES);
        long wfoToday = attendanceRecordRepository.countByDateAndMode(date, AttendanceMode.WFO);
        long wfhToday = attendanceRecordRepository.countByDateAndMode(date, AttendanceMode.WFH);
        double averageWfoPercentage = presentToday > 0 ? (wfoToday * 100.0 / presentToday) : 0.0;

        LeadershipDashboardResponse.OrganizationKpis kpis = LeadershipDashboardResponse.OrganizationKpis.builder()
                .totalEmployees(totalEmployees)
                .presentToday(presentToday)
                .wfoToday(wfoToday)
                .wfhToday(wfhToday)
                .averageWfoPercentage(averageWfoPercentage)
                .build();

        List<LeadershipDashboardResponse.TrendPoint> trend = buildTrend(date.minusDays(14), date);
        List<LeadershipDashboardResponse.TeamPerformanceRow> teamPerformance =
                buildTeamPerformance(date.minusDays(30), date);

        LeadershipDashboardResponse response = LeadershipDashboardResponse.builder()
                .kpis(kpis)
                .wfoWfhTrend(trend)
                .teamPerformance(teamPerformance)
                .build();

        cacheResponse(cacheKey, response);
        return response;
    }

    private List<LeadershipDashboardResponse.TrendPoint> buildTrend(LocalDate from, LocalDate to) {
        List<Object[]> rows = attendanceRecordRepository.wfoWfhTrend(from, to);
        List<LeadershipDashboardResponse.TrendPoint> trend = new ArrayList<>();
        for (Object[] row : rows) {
            trend.add(LeadershipDashboardResponse.TrendPoint.builder()
                    .date(row[0].toString())
                    .wfoCount((Long) row[1])
                    .wfhCount((Long) row[2])
                    .build());
        }
        return trend;
    }

    private List<LeadershipDashboardResponse.TeamPerformanceRow> buildTeamPerformance(LocalDate from, LocalDate to) {
        long workingDays = WorkingDayUtils.countWeekdaysInclusive(from, to);
        List<Object[]> rows = attendanceRecordRepository.teamAttendancePercentagesWithNames(from, to, workingDays);
        List<LeadershipDashboardResponse.TeamPerformanceRow> performance = new ArrayList<>();
        for (Object[] row : rows) {
            Long teamId = (Long) row[0];
            String teamName = (String) row[1];
            Double percentage = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            performance.add(LeadershipDashboardResponse.TeamPerformanceRow.builder()
                    .teamId(teamId)
                    .teamName(teamName)
                    .attendancePercentage(percentage)
                    .build());
        }
        return performance;
    }

    private void cacheResponse(String cacheKey, LeadershipDashboardResponse response) {
        try {
            cacheAdapter.put(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(dashboardTtlSeconds));
        } catch (Exception ex) {
            log.warn("Failed to cache leadership dashboard for key {}", cacheKey, ex);
        }
    }

    private Optional<LeadershipDashboardResponse> deserialize(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, LeadershipDashboardResponse.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
