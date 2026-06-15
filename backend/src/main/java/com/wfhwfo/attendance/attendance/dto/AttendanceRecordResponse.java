package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Attendance record without raw geo coordinates for privacy")
public class AttendanceRecordResponse {

    private Long id;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private AttendanceMode attendanceMode;
    private CurrentSessionStatus currentSessionStatus;
    private AttendanceStatus status;
    private ProcessingStatus processingStatus;
    private Boolean late;
    private Long matchedOfficeLocationId;
    private Double distanceFromOfficeMeters;
    private Integer totalOfficeMinutes;
    private String source;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
