package com.wfhwfo.attendance.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoTrackingSessionState {

    private Boolean wasInside;
    private LocalDateTime insideSince;
    private LocalDateTime outsideSince;
    private Boolean wfhPromptDismissed;
}
