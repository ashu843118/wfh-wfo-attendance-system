package com.wfhwfo.attendance.attendance.controller;

import com.wfhwfo.attendance.attendance.dto.AttendanceActionResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceRecordResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceEventResponse;
import com.wfhwfo.attendance.attendance.dto.AutoAttendanceEventRequest;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.dto.LocationSignalResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceSessionResponse;
import com.wfhwfo.attendance.attendance.dto.CheckInRequest;
import com.wfhwfo.attendance.attendance.dto.CheckOutRequest;
import com.wfhwfo.attendance.attendance.service.AttendanceService;
import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Employee check-in, check-out, and attendance history")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/location-signal")
    @Operation(summary = "Process active-session location signal for auto attendance detection")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<LocationSignalResponse>> processLocationSignal(
            @Valid @RequestBody LocationPayload request) {
        LocationSignalResponse response = attendanceService.processLocationSignal(request);
        return ResponseEntity.ok(ApiResponse.success("Location signal processed", response));
    }

    @PostMapping("/check-in")
    @Operation(summary = "Check in with geo location", description = "Returns quickly with CLASSIFICATION_PENDING; WFO/WFH resolved asynchronously")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<AttendanceActionResponse>> checkIn(@Valid @RequestBody CheckInRequest request) {
        AttendanceActionResponse response = attendanceService.checkIn(request);
        return ResponseEntity.ok(ApiResponse.success("Check-in recorded", response));
    }

    @PostMapping("/wfh-check-in")
    @Operation(summary = "Confirm WFH check-in when outside assigned office geofence")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<AttendanceActionResponse>> confirmWfhCheckIn(
            @Valid @RequestBody CheckInRequest request) {
        AttendanceActionResponse response = attendanceService.confirmWfhCheckIn(request);
        return ResponseEntity.ok(ApiResponse.success("WFH check-in recorded", response));
    }

    @PostMapping("/dismiss-wfh-prompt")
    @Operation(summary = "Dismiss WFH confirmation prompt for today")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<Void>> dismissWfhPrompt() {
        attendanceService.dismissWfhPrompt();
        return ResponseEntity.ok(ApiResponse.success("WFH prompt dismissed", null));
    }

    @PostMapping("/check-out")
    @Operation(summary = "Check out with geo location")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<AttendanceActionResponse>> checkOut(@Valid @RequestBody CheckOutRequest request) {
        AttendanceActionResponse response = attendanceService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success("Check-out recorded", response));
    }

    @PostMapping("/events/auto")
    @Operation(summary = "Record auto geofence enter/exit event (PWA auto mode)")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<AttendanceActionResponse>> recordAutoEvent(
            @Valid @RequestBody AutoAttendanceEventRequest request) {
        AttendanceActionResponse response = attendanceService.recordAutoEvent(request);
        return ResponseEntity.ok(ApiResponse.success("Auto attendance event recorded", response));
    }

    @GetMapping({"/history", "/me"})
    @Operation(summary = "Get paginated attendance history for the current employee")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceRecordResponse>>> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AttendanceRecordResponse> response = attendanceService.getHistory(from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Attendance history fetched successfully", response));
    }

    @GetMapping({"/events", "/me/events"})
    @Operation(summary = "Get paginated attendance events for a date or date range")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceEventResponse>>> getEvents(
            @Parameter(description = "Single date for session/event detail view")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "eventTime", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<AttendanceEventResponse> response = date != null
                ? attendanceService.getEventsForDate(date, pageable)
                : attendanceService.getEventHistory(from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Attendance events fetched", response));
    }

    @GetMapping("/me/events/{date}")
    @Operation(summary = "Get all attendance events for a specific date (legacy, non-paginated)")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<List<AttendanceEventResponse>>> getEventsForDateLegacy(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceEventResponse> response = attendanceService.getEventsForDateList(date);
        return ResponseEntity.ok(ApiResponse.success("Attendance events for date fetched", response));
    }

    @GetMapping({"/sessions", "/me/sessions"})
    @Operation(summary = "Get paginated attendance sessions for a specific date")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceSessionResponse>>> getSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20, sort = "checkInTime", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<AttendanceSessionResponse> response = attendanceService.getSessionsForDate(date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Attendance sessions fetched", response));
    }

    @GetMapping("/me/sessions/{date}")
    @Operation(summary = "Get all attendance sessions for a specific date (legacy, non-paginated)")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<List<AttendanceSessionResponse>>> getSessionsForDateLegacy(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceSessionResponse> response = attendanceService.getSessionsForDateList(date);
        return ResponseEntity.ok(ApiResponse.success("Attendance sessions for date fetched", response));
    }

    @GetMapping("/me/today")
    @Operation(summary = "Get today's attendance record for the current employee")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> getToday() {
        AttendanceRecordResponse response = attendanceService.getTodayForEmployee();
        return ResponseEntity.ok(ApiResponse.success("Today's attendance fetched", response));
    }
}
