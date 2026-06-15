package com.wfhwfo.attendance.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePolicyResponse {

    private Long id;
    private Long teamId;
    private Integer minimumWfoDaysPerWeek;
    private LocalTime standardCheckInTime;
    private LocalTime standardCheckOutTime;
    private Integer lateThresholdMinutes;
    private Integer requiredWfoMinutes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
