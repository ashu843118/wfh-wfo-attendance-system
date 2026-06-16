package com.wfhwfo.attendance.dashboard.controller;

import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.dashboard.dto.LeadershipDashboardResponse;
import com.wfhwfo.attendance.dashboard.service.LeadershipDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/leadership")
@RequiredArgsConstructor
@Tag(name = "Leadership Dashboard", description = "Organization-wide attendance analytics")
@SecurityRequirement(name = "bearerAuth")
public class LeadershipDashboardController {

    private final LeadershipDashboardService leadershipDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get organization-wide attendance dashboard")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<LeadershipDashboardResponse>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LeadershipDashboardResponse response = leadershipDashboardService.getDashboard(date);
        return ResponseEntity.ok(ApiResponse.success("Leadership dashboard fetched", response));
    }

    @GetMapping("/team-summary")
    @Operation(summary = "Get paginated team performance summary")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<LeadershipDashboardResponse.TeamPerformanceRow>>> getTeamSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadershipDashboardResponse.TeamPerformanceRow> response =
                leadershipDashboardService.getTeamSummary(date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Team summary fetched", response));
    }
}
