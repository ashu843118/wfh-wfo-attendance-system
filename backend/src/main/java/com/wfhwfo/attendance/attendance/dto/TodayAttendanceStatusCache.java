package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
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
public class TodayAttendanceStatusCache {

    private Long id;
    private LocalDate attendanceDate;
    private AttendanceMode attendanceMode;
    private AttendanceMode currentSessionMode;
    private CurrentSessionStatus currentSessionStatus;
    private AttendanceStatus status;
    private ProcessingStatus processingStatus;
    private Boolean canCheckIn;
    private Boolean canCheckOut;
    private LocalDateTime firstCheckInTime;
    private LocalDateTime finalCheckoutTime;
    private Integer totalOfficeMinutes;
    private Long matchedOfficeLocationId;
    private Double distanceFromOfficeMeters;
}
