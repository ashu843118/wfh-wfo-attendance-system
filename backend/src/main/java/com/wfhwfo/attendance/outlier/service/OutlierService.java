package com.wfhwfo.attendance.outlier.service;

import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.dto.PagedResponseMapper;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.outlier.dto.OutlierResponse;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutlierService {

    private final AttendanceOutlierRepository attendanceOutlierRepository;

    @Transactional(readOnly = true)
    public PagedResponse<OutlierResponse> searchOutliers(
            OutlierStatus status,
            Severity severity,
            OutlierType type,
            Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        Long managerScope = user.getRole() == Role.MANAGER ? user.getEmployeeId() : null;

        Page<AttendanceOutlier> page = attendanceOutlierRepository.searchOutliers(
                status, severity, type, managerScope, pageable);

        return PagedResponseMapper.from(page, this::toResponse);
    }

    private OutlierResponse toResponse(AttendanceOutlier outlier) {
        return OutlierResponse.builder()
                .id(outlier.getId())
                .employeeId(outlier.getEmployeeId())
                .teamId(outlier.getTeamId())
                .attendanceRecordId(outlier.getAttendanceRecordId())
                .outlierType(outlier.getOutlierType())
                .severity(outlier.getSeverity())
                .description(outlier.getDescription())
                .status(outlier.getStatus())
                .detectedAt(outlier.getDetectedAt())
                .build();
    }
}
