package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceEventType;
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
public class AttendanceEventResponse {

    private Long id;
    private LocalDate attendanceDate;
    private AttendanceEventType eventType;
    private LocalDateTime eventTime;
    private AttendanceTriggerMode triggerMode;
    private String source;
    private Double latitude;
    private Double longitude;
    private boolean valid;
}
