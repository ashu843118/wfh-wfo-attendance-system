package com.wfhwfo.attendance.dashboard.dto;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDrilldownDto {

    private Long employeeId;
    private String employeeName;
    private String email;
    private String assignedOfficeName;
    private String todayStatus;
    private String attendanceMode;
    private LocalDateTime firstCheckInTime;
    private LocalDateTime finalCheckOutTime;
    private Integer totalOfficeMinutes;
    private String currentSessionStatus;
    private Long outlierCount;

    private Long outlierId;
    private OutlierType outlierType;
    private Severity severity;
    private String description;
    private LocalDateTime detectedAt;
    private OutlierStatus outlierStatus;
}
