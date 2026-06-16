package com.wfhwfo.attendance.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAttendanceRowResponse {

    private Long employeeId;
    private String employeeName;
    private String status;
    private String mode;
    private Boolean late;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
}
