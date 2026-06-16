package com.wfhwfo.attendance.outlier.controller;

import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import com.wfhwfo.attendance.outlier.dto.OutlierResponse;
import com.wfhwfo.attendance.outlier.service.OutlierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outliers")
@RequiredArgsConstructor
@Tag(name = "Outliers", description = "Attendance outlier alerts with filters and pagination")
@SecurityRequirement(name = "bearerAuth")
public class OutlierController {

    private final OutlierService outlierService;

    @GetMapping
    @Operation(summary = "Get paginated outlier alerts with optional filters")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<OutlierResponse>>> searchOutliers(
            @Parameter(description = "Filter by outlier status")
            @RequestParam(required = false) OutlierStatus status,
            @Parameter(description = "Filter by severity")
            @RequestParam(required = false) Severity severity,
            @Parameter(description = "Filter by outlier type")
            @RequestParam(required = false) OutlierType type,
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<OutlierResponse> response = outlierService.searchOutliers(status, severity, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Outliers fetched", response));
    }
}
