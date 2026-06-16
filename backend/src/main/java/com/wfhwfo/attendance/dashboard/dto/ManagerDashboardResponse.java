package com.wfhwfo.attendance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {

    private KpiSummary kpis;
    private List<ChartSlice> modePieChart;
    private List<BarChartPoint> monthlyBarChart;
    private List<TeamAttendanceRow> teamTable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiSummary {
        private long teamSize;
        private long presentToday;
        private long absentToday;
        private long wfoToday;
        private long wfhToday;
        private long lateToday;
        private long pendingClassification;
        private long openOutliers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartSlice {
        private String label;
        private long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarChartPoint {
        private String employeeName;
        private long presentDays;
        private long wfoDays;
        private long wfhDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamAttendanceRow {
        private Long employeeId;
        private String employeeName;
        private String status;
        private String mode;
        private Boolean late;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
    }
}
