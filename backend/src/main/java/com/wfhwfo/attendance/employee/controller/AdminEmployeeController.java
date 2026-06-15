package com.wfhwfo.attendance.employee.controller;

import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.employee.dto.CreateEmployeeRequest;
import com.wfhwfo.attendance.employee.dto.EmployeeAdminResponse;
import com.wfhwfo.attendance.employee.dto.ManagerOptionResponse;
import com.wfhwfo.attendance.employee.dto.UpdateEmployeeRequest;
import com.wfhwfo.attendance.employee.dto.UpdateEmployeeStatusRequest;
import com.wfhwfo.attendance.employee.service.EmployeeAdminService;
import com.wfhwfo.attendance.team.dto.TeamOptionResponse;
import com.wfhwfo.attendance.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Configuration", description = "Admin-controlled employee provisioning")
@SecurityRequirement(name = "bearerAuth")
public class AdminEmployeeController {

    private final EmployeeAdminService employeeAdminService;
    private final TeamService teamService;

    @GetMapping("/employees")
    @Operation(summary = "List employees with filters and pagination")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeAdminResponse>>> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Boolean active) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        PagedResponse<EmployeeAdminResponse> result = employeeAdminService.listEmployees(
                search, role, teamId, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("Employees fetched successfully", result));
    }

    @PostMapping("/employees")
    @Operation(summary = "Create a new employee with temporary password")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeAdminResponse response = employeeAdminService.createEmployee(request);
        return ResponseEntity.ok(ApiResponse.success("Employee created successfully", response));
    }

    @PutMapping("/employees/{id}")
    @Operation(summary = "Update employee details")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        EmployeeAdminResponse response = employeeAdminService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @PatchMapping("/employees/{id}/status")
    @Operation(summary = "Activate or deactivate an employee")
    public ResponseEntity<ApiResponse<EmployeeAdminResponse>> updateEmployeeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        EmployeeAdminResponse response = employeeAdminService.updateEmployeeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee status updated successfully", response));
    }

    @GetMapping("/managers")
    @Operation(summary = "List active managers for dropdown")
    public ResponseEntity<ApiResponse<List<ManagerOptionResponse>>> listManagers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Managers fetched successfully",
                employeeAdminService.getActiveManagers()));
    }

    @GetMapping("/teams")
    @Operation(summary = "List teams for dropdown")
    public ResponseEntity<ApiResponse<List<TeamOptionResponse>>> listTeams() {
        return ResponseEntity.ok(ApiResponse.success(
                "Teams fetched successfully",
                teamService.getAllTeams()));
    }
}
