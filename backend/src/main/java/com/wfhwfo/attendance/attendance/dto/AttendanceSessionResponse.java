package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
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
public class AttendanceSessionResponse {

    private Long id;
    private LocalDate attendanceDate;
    private AttendanceMode sessionMode;
    private AttendanceEventType checkInEventType;
    private LocalDateTime checkInTime;
    private AttendanceTriggerMode checkInTriggerMode;
    private LocalDateTime checkOutTime;
    private AttendanceEventType checkOutEventType;
    private boolean autoCheckoutEligible;
    private AttendanceSessionStatus status;
    private Long matchedOfficeLocationId;
}
