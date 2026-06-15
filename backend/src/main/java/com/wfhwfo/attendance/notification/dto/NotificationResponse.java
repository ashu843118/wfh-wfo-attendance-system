package com.wfhwfo.attendance.notification.dto;

import com.wfhwfo.attendance.common.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long recipientEmployeeId;
    private Long relatedEmployeeId;
    private String title;
    private String message;
    private String type;
    private Severity severity;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
