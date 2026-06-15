package com.wfhwfo.attendance.outbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventPayload {

    private Long employeeId;
    private Long teamId;
    private Long attendanceRecordId;
    private Long attendanceEventId;
    private String action;
    private Long managerId;
}
