package com.wfhwfo.attendance.dashboard.controller;

import com.wfhwfo.attendance.attendance.dto.AttendanceRecordResponse;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardResponse;
import com.wfhwfo.attendance.dashboard.service.ManagerDashboardService;
import com.wfhwfo.attendance.outlier.dto.OutlierResponse;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Tag(name = "Manager Dashboard", description = "Manager team attendance and outlier dashboards")
@SecurityRequirement(name = "bearerAuth")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Get manager dashboard KPIs, charts, and team table")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<ManagerDashboardResponse>> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ManagerDashboardResponse response = managerDashboardService.getDashboardSummary(date);
        return ResponseEntity.ok(ApiResponse.success("Manager dashboard fetched", response));
    }

    @GetMapping("/team-attendance")
    @Operation(summary = "Get paginated team attendance for a given date")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceRecordResponse>>> getTeamAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceRecord> page = managerDashboardService.getTeamAttendance(date, pageable);
        List<AttendanceRecordResponse> content = page.getContent().stream()
                .map(this::toRecordResponse)
                .toList();
        PagedResponse<AttendanceRecordResponse> response = PagedResponse.<AttendanceRecordResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Team attendance fetched", response));
    }

    @GetMapping("/outliers")
    @Operation(summary = "Get paginated open outliers for the manager's team")
    public ResponseEntity<ApiResponse<PagedResponse<OutlierResponse>>> getOutliers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceOutlier> page = managerDashboardService.getOutliers(pageable);
        List<OutlierResponse> content = page.getContent().stream()
                .map(this::toOutlierResponse)
                .toList();
        PagedResponse<OutlierResponse> response = PagedResponse.<OutlierResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Outliers fetched", response));
    }

    @GetMapping("/employees/{employeeId}/attendance")
    @Operation(summary = "Get paginated attendance history for a team member")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceRecordResponse>>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AttendanceRecord> page = managerDashboardService.getEmployeeAttendance(employeeId, from, to, pageable);
        List<AttendanceRecordResponse> content = page.getContent().stream()
                .map(this::toRecordResponse)
                .toList();
        PagedResponse<AttendanceRecordResponse> response = PagedResponse.<AttendanceRecordResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Employee attendance fetched", response));
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .attendanceDate(record.getAttendanceDate())
                .checkInTime(record.getFirstCheckInTime())
                .checkOutTime(record.getFinalCheckOutTime())
                .attendanceMode(record.getAttendanceMode())
                .status(record.getStatus())
                .processingStatus(record.getProcessingStatus())
                .late(record.getLate())
                .source(record.getSource())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private OutlierResponse toOutlierResponse(AttendanceOutlier outlier) {
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
