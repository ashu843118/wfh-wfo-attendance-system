package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.dto.AttendanceActionResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceEventResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceRecordResponse;
import com.wfhwfo.attendance.attendance.dto.AutoAttendanceEventRequest;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.dto.LocationSignalResponse;
import com.wfhwfo.attendance.attendance.dto.CheckInRequest;
import com.wfhwfo.attendance.attendance.dto.CheckOutRequest;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.LockAdapter;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final long LOCK_WAIT_MS = 3_000;
    private static final long LOCK_LEASE_MS = 15_000;

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceWriteService attendanceWriteService;
    private final LocationSignalService locationSignalService;
    private final LockAdapter lockAdapter;

    public AttendanceActionResponse checkIn(CheckInRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate today = LocalDate.now();
        String lockKey = "attendance:events:" + user.getEmployeeId() + ":" + today;

        AttendanceActionResponse[] responseHolder = new AttendanceActionResponse[1];
        lockAdapter.executeWithLock(lockKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                responseHolder[0] = attendanceWriteService.checkIn(user, today, request));
        return responseHolder[0];
    }

    public AttendanceActionResponse checkOut(CheckOutRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate today = LocalDate.now();
        String lockKey = "attendance:events:" + user.getEmployeeId() + ":" + today;

        AttendanceActionResponse[] responseHolder = new AttendanceActionResponse[1];
        lockAdapter.executeWithLock(lockKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                responseHolder[0] = attendanceWriteService.checkOut(user, today, request));
        return responseHolder[0];
    }

    public AttendanceActionResponse recordAutoEvent(AutoAttendanceEventRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate today = LocalDate.now();
        String lockKey = "attendance:events:" + user.getEmployeeId() + ":" + today;

        AttendanceActionResponse[] responseHolder = new AttendanceActionResponse[1];
        lockAdapter.executeWithLock(lockKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                responseHolder[0] = attendanceWriteService.recordAutoEvent(user, today, request));
        return responseHolder[0];
    }

    public com.wfhwfo.attendance.attendance.dto.LocationSignalResponse processLocationSignal(LocationPayload location) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate today = LocalDate.now();
        String lockKey = "attendance:location-signal:" + user.getEmployeeId() + ":" + today;

        com.wfhwfo.attendance.attendance.dto.LocationSignalResponse[] responseHolder =
                new com.wfhwfo.attendance.attendance.dto.LocationSignalResponse[1];
        lockAdapter.executeWithLock(lockKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                responseHolder[0] = locationSignalService.processLocationSignal(user, location));
        return responseHolder[0];
    }

    public AttendanceActionResponse confirmWfhCheckIn(CheckInRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate today = LocalDate.now();
        String lockKey = "attendance:events:" + user.getEmployeeId() + ":" + today;

        AttendanceActionResponse[] responseHolder = new AttendanceActionResponse[1];
        lockAdapter.executeWithLock(lockKey, LOCK_WAIT_MS, LOCK_LEASE_MS, () ->
                responseHolder[0] = attendanceWriteService.confirmWfhCheckIn(user, today, request));
        return responseHolder[0];
    }

    public void dismissWfhPrompt() {
        UserPrincipal user = SecurityUtils.currentUser();
        locationSignalService.dismissWfhPrompt(user.getEmployeeId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public AttendanceRecordResponse getTodayForEmployee() {
        UserPrincipal user = SecurityUtils.currentUser();
        return attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), LocalDate.now())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendanceRecordResponse> getHistory(LocalDate from, LocalDate to, Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate fromDate = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        Page<AttendanceRecord> page = attendanceRecordRepository.findByEmployeeIdAndAttendanceDateBetween(
                user.getEmployeeId(), fromDate, toDate, pageable);

        List<AttendanceRecordResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<AttendanceRecordResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendanceEventResponse> getEventHistory(LocalDate from, LocalDate to, Pageable pageable) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate fromDate = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate toDate = to != null ? to : LocalDate.now();

        Page<com.wfhwfo.attendance.attendance.entity.AttendanceEvent> page =
                attendanceEventRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByEventTimeDescIdDesc(
                        user.getEmployeeId(), fromDate, toDate, pageable);

        List<AttendanceEventResponse> content = page.getContent().stream()
                .map(this::toEventResponse)
                .toList();

        return PagedResponse.<AttendanceEventResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceEventResponse> getEventsForDate(LocalDate date) {
        UserPrincipal user = SecurityUtils.currentUser();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return attendanceEventRepository.findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(
                        user.getEmployeeId(), targetDate).stream()
                .map(this::toEventResponse)
                .toList();
    }

    private AttendanceEventResponse toEventResponse(com.wfhwfo.attendance.attendance.entity.AttendanceEvent event) {
        return AttendanceEventResponse.builder()
                .id(event.getId())
                .attendanceDate(event.getAttendanceDate())
                .eventType(event.getEventType())
                .eventTime(event.getEventTime())
                .triggerMode(event.getTriggerMode())
                .source(event.getSource())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .valid(event.isValid())
                .build();
    }

    private AttendanceRecordResponse toResponse(AttendanceRecord record) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .attendanceDate(record.getAttendanceDate())
                .checkInTime(record.getFirstCheckInTime())
                .checkOutTime(record.getFinalCheckOutTime())
                .attendanceMode(record.getAttendanceMode())
                .currentSessionStatus(record.getCurrentSessionStatus())
                .status(record.getStatus())
                .processingStatus(record.getProcessingStatus())
                .late(record.getLate())
                .matchedOfficeLocationId(record.getMatchedOfficeLocationId())
                .distanceFromOfficeMeters(record.getDistanceFromOfficeMeters())
                .totalOfficeMinutes(record.getTotalOfficeMinutes())
                .source(record.getSource())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
