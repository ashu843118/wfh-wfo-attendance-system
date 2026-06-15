package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fast response returned immediately after check-in or check-out")
public class AttendanceActionResponse {

    private Long attendanceId;
    private Long eventId;
    private AttendanceStatus status;
    private ProcessingStatus processingStatus;
    private LocalDateTime recordedAt;
}
