package com.wfhwfo.attendance.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update an attendance policy")
public class AttendancePolicyRequest {

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotNull(message = "Minimum WFO days per week is required")
    @Min(0)
    private Integer minimumWfoDaysPerWeek;

    @NotNull(message = "Standard check-in time is required")
    private LocalTime standardCheckInTime;

    @NotNull(message = "Standard check-out time is required")
    private LocalTime standardCheckOutTime;

    @NotNull(message = "Late threshold minutes is required")
    @Min(0)
    private Integer lateThresholdMinutes;

    @NotNull(message = "Required WFO minutes is required")
    @Min(1)
    private Integer requiredWfoMinutes;

    private boolean active = true;
}
