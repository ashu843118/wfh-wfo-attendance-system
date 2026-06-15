package com.wfhwfo.attendance.dashboard.controller;

import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.dashboard.dto.EmployeeDashboardResponse;
import com.wfhwfo.attendance.dashboard.service.EmployeeDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@Tag(name = "Employee Dashboard", description = "Personal attendance dashboard for employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeDashboardController {

    private final EmployeeDashboardService employeeDashboardService;

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Get employee personal dashboard summary")
    public ResponseEntity<ApiResponse<EmployeeDashboardResponse>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Employee dashboard loaded successfully",
                employeeDashboardService.getDashboard()));
    }
}
