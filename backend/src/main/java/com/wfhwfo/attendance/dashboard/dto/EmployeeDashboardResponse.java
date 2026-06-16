package com.wfhwfo.attendance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDashboardResponse {

    private EmployeeKpis kpis;
    private AssignedOfficeInfo assignedOffice;
    private List<TrendPoint> wfoWfhTrend;
    private List<RecentAttendanceRow> recentAttendance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedOfficeInfo {
        private Long id;
        private String officeName;
        private String address;
        private Double latitude;
        private Double longitude;
        private Integer radiusMeters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeKpis {
        private long presentDaysThisMonth;
        private long wfoDaysThisMonth;
        private long wfhDaysThisMonth;
        private long lateDaysThisMonth;
        private long pendingClassification;
        private long missingCheckoutDaysThisMonth;
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
    public static class RecentAttendanceRow {
        private String date;
        private String status;
        private String mode;
        private Boolean late;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
    }
}
