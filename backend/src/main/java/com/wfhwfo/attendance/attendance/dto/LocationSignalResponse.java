package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AutoTrackingStateLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Auto attendance detection state after processing a location signal")
public class LocationSignalResponse {

    private AutoTrackingStateLabel trackingState;
    private boolean insideOffice;
    private boolean locationReliable;
    private String assignedOfficeName;
    private String matchedOfficeName;
    private Double distanceFromOfficeMeters;
    private Long checkInStableSecondsRemaining;
    private Long graceSecondsRemaining;
    private boolean requiresWfhConfirmation;
    private String userMessage;
    private AttendanceRecordResponse todaySummary;
    private AttendanceActionResponse actionTaken;
}
