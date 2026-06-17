package com.wfhwfo.attendance.dashboard.controller;

import com.wfhwfo.attendance.attendance.dto.AttendanceRecordResponse;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.dto.PagedResponseMapper;
import com.wfhwfo.attendance.config.PaginationConfig;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardDrilldownDto;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardDrilldownType;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardResponse;
import com.wfhwfo.attendance.dashboard.dto.TeamAttendanceRowResponse;
import com.wfhwfo.attendance.dashboard.service.ManagerDashboardService;
import com.wfhwfo.attendance.outlier.dto.OutlierResponse;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    @GetMapping("/dashboard/drilldown")
    @Operation(summary = "Get paginated KPI drill-down details for the manager dashboard")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<ManagerDashboardDrilldownDto>>> getDrilldown(
            @RequestParam ManagerDashboardDrilldownType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int effectiveSize = Math.min(Math.max(size, 1), PaginationConfig.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);
        PagedResponse<ManagerDashboardDrilldownDto> response =
                managerDashboardService.getDrilldown(type, date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Dashboard drill-down fetched", response));
    }

    @GetMapping("/team-attendance")
    @Operation(summary = "Get paginated team attendance for a given date")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<TeamAttendanceRowResponse>>> getTeamAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<TeamAttendanceRowResponse> response =
                managerDashboardService.getTeamAttendanceRows(date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Team attendance fetched", response));
    }

    @GetMapping("/outliers")
    @Operation(summary = "Get paginated open outliers for the manager's team")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<OutlierResponse>>> getOutliers(
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttendanceOutlier> page = managerDashboardService.getOutliers(pageable);
        PagedResponse<OutlierResponse> response = PagedResponseMapper.from(page, this::toOutlierResponse);
        return ResponseEntity.ok(ApiResponse.success("Outliers fetched", response));
    }

    @GetMapping("/employees/{employeeId}/attendance")
    @Operation(summary = "Get paginated attendance history for a team member")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceRecordResponse>>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttendanceRecord> page = managerDashboardService.getEmployeeAttendance(employeeId, from, to, pageable);
        PagedResponse<AttendanceRecordResponse> response = PagedResponseMapper.from(page, this::toRecordResponse);
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
