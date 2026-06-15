package com.wfhwfo.attendance.outlier.dto;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
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
public class OutlierResponse {

    private Long id;
    private Long employeeId;
    private Long teamId;
    private Long attendanceRecordId;
    private OutlierType outlierType;
    private Severity severity;
    private String description;
    private OutlierStatus status;
    private LocalDateTime detectedAt;
}
