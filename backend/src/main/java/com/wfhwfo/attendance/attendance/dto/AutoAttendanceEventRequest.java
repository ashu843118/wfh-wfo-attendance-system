package com.wfhwfo.attendance.attendance.dto;

import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAttendanceEventRequest {

    @NotNull
    private AttendanceEventType eventType;

    @NotNull
    @Valid
    private LocationPayload location;

    private String source;
}
