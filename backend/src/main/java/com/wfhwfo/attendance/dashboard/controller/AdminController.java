package com.wfhwfo.attendance.dashboard.controller;

import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.office.dto.OfficeLocationRequest;
import com.wfhwfo.attendance.office.dto.OfficeLocationResponse;
import com.wfhwfo.attendance.office.service.OfficeLocationService;
import com.wfhwfo.attendance.policy.dto.AttendancePolicyRequest;
import com.wfhwfo.attendance.policy.dto.AttendancePolicyResponse;
import com.wfhwfo.attendance.policy.service.AttendancePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Configuration", description = "Office location and attendance policy administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final OfficeLocationService officeLocationService;
    private final AttendancePolicyService attendancePolicyService;

    @GetMapping({"/office-locations", "/offices"})
    @Operation(summary = "List office locations with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<OfficeLocationResponse>>> listOffices(
            @PageableDefault(size = 20, sort = "officeName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Office locations fetched", officeLocationService.list(pageable)));
    }

    @GetMapping("/office-locations/active")
    @Operation(summary = "List all active office locations for dropdowns")
    public ResponseEntity<ApiResponse<List<OfficeLocationResponse>>> listActiveOffices() {
        return ResponseEntity.ok(ApiResponse.success("Active office locations fetched", officeLocationService.getActiveOffices()));
    }

    @GetMapping("/office-locations/{id}")
    @Operation(summary = "Get office location by ID")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> getOffice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Office location fetched", officeLocationService.getById(id)));
    }

    @PostMapping("/office-locations")
    @Operation(summary = "Create a new office location")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> createOffice(
            @Valid @RequestBody OfficeLocationRequest request) {
        OfficeLocationResponse response = officeLocationService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Office location created", response));
    }

    @PutMapping("/office-locations/{id}")
    @Operation(summary = "Update an office location")
    public ResponseEntity<ApiResponse<OfficeLocationResponse>> updateOffice(
            @PathVariable Long id,
            @Valid @RequestBody OfficeLocationRequest request) {
        OfficeLocationResponse response = officeLocationService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Office location updated", response));
    }

    @DeleteMapping("/office-locations/{id}")
    @Operation(summary = "Delete an office location")
    public ResponseEntity<ApiResponse<Void>> deleteOffice(@PathVariable Long id) {
        officeLocationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Office location deleted", null));
    }

    @GetMapping("/policies")
    @Operation(summary = "List attendance policies with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<AttendancePolicyResponse>>> listPolicies(
            @PageableDefault(size = 20, sort = "teamId", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Policies fetched", attendancePolicyService.list(pageable)));
    }

    @GetMapping("/policies/{id}")
    @Operation(summary = "Get attendance policy by ID")
    public ResponseEntity<ApiResponse<AttendancePolicyResponse>> getPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Policy fetched", attendancePolicyService.getById(id)));
    }

    @PostMapping("/policies")
    @Operation(summary = "Create a new attendance policy")
    public ResponseEntity<ApiResponse<AttendancePolicyResponse>> createPolicy(
            @Valid @RequestBody AttendancePolicyRequest request) {
        AttendancePolicyResponse response = attendancePolicyService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Policy created", response));
    }

    @PutMapping("/policies/{id}")
    @Operation(summary = "Update an attendance policy")
    public ResponseEntity<ApiResponse<AttendancePolicyResponse>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody AttendancePolicyRequest request) {
        AttendancePolicyResponse response = attendancePolicyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Policy updated", response));
    }

    @DeleteMapping("/policies/{id}")
    @Operation(summary = "Delete an attendance policy")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        attendancePolicyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Policy deleted", null));
    }
}
