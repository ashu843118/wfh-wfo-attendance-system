package com.wfhwfo.attendance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadershipDashboardResponse {

    private OrganizationKpis kpis;
    private List<TrendPoint> wfoWfhTrend;
    private List<TeamPerformanceRow> teamPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationKpis {
        private long totalEmployees;
        private long presentToday;
        private long wfoToday;
        private long wfhToday;
        private double averageWfoPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private long wfoCount;
        private long wfhCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamPerformanceRow {
        private Long teamId;
        private String teamName;
        private double attendancePercentage;
    }
}
