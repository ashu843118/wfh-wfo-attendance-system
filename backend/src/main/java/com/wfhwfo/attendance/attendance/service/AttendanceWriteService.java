package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.dto.AttendanceActionResponse;
import com.wfhwfo.attendance.attendance.dto.AutoAttendanceEventRequest;
import com.wfhwfo.attendance.attendance.dto.CheckInRequest;
import com.wfhwfo.attendance.attendance.dto.CheckOutRequest;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.attendance.util.GeoPointUtils;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.service.AssignedOfficeGeofenceService;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceWriteService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceSessionService attendanceSessionService;
    private final DailySummaryService dailySummaryService;
    private final OutboxService outboxService;
    private final EmployeeOfficeCacheService employeeOfficeCacheService;
    private final AssignedOfficeGeofenceService assignedOfficeGeofenceService;

    @Transactional
    public AttendanceActionResponse checkIn(UserPrincipal user, LocalDate today, CheckInRequest request) {
        assertNoOpenSession(user.getEmployeeId(), today);
        GeoFenceMatchResult match = resolveGeofenceMatch(user.getEmployeeId(), request.getLocation());
        return recordManualEvent(
                user, today, AttendanceEventType.MANUAL_CHECK_IN, request.getLocation(), request.getSource(), match);
    }

    @Transactional
    public AttendanceActionResponse confirmWfhCheckIn(UserPrincipal user, LocalDate today, CheckInRequest request) {
        assertNoOpenSession(user.getEmployeeId(), today);

        GeoFenceMatchResult match = resolveGeofenceMatch(user.getEmployeeId(), request.getLocation());
        if (match.isWithinFence()) {
            throw new BusinessException(
                    "Cannot confirm WFH while inside assigned office geofence",
                    "ATTENDANCE_WFH_INSIDE_OFFICE");
        }

        String source = request.getSource() != null ? request.getSource() : "PWA";
        return recordEvent(
                user,
                today,
                getOrCreateSummary(user, today),
                AttendanceEventType.WFH_CONFIRMED_CHECK_IN,
                AttendanceTriggerMode.MANUAL,
                request.getLocation(),
                source,
                match);
    }

    @Transactional
    public AttendanceActionResponse checkOut(UserPrincipal user, LocalDate today, CheckOutRequest request) {
        AttendanceSession openSession = requireOpenSession(user.getEmployeeId(), today);
        AttendanceRecord summary = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                .orElseThrow(() -> new BusinessException("No check-in found for today", "ATTENDANCE_NO_CHECKIN"));

        GeoFenceMatchResult match = resolveGeofenceMatch(user.getEmployeeId(), request.getLocation());
        String source = request.getSource() != null ? request.getSource() : "PWA";
        return recordEvent(user, today, summary, AttendanceEventType.MANUAL_CHECK_OUT, AttendanceTriggerMode.MANUAL,
                request.getLocation(), source, match, openSession);
    }

    @Transactional
    public AttendanceActionResponse recordTrackedEvent(
            UserPrincipal user,
            LocalDate today,
            AttendanceEventType eventType,
            AttendanceTriggerMode triggerMode,
            LocationPayload location,
            String source,
            GeoFenceMatchResult geofenceMatch) {
        AttendanceRecord summary = getOrCreateSummary(user, today);

        if (isCheckInLikeEvent(eventType)) {
            assertNoOpenSession(user.getEmployeeId(), today);
        }

        AttendanceSession openSession = null;
        if (isCheckOutLikeEvent(eventType)) {
            openSession = requireOpenSession(user.getEmployeeId(), today);
            if (!openSession.isAutoCheckoutEligible()) {
                throw new BusinessException(
                        "Auto checkout is not allowed for this session",
                        "ATTENDANCE_AUTO_CHECKOUT_NOT_ALLOWED");
            }
        }

        return recordEvent(user, today, summary, eventType, triggerMode, location, source, geofenceMatch, openSession);
    }

    @Transactional
    public AttendanceActionResponse recordAutoEvent(UserPrincipal user, LocalDate today, AutoAttendanceEventRequest request) {
        AttendanceEventType eventType = request.getEventType();
        if (eventType != AttendanceEventType.ENTERED_GEOFENCE && eventType != AttendanceEventType.EXITED_GEOFENCE) {
            throw new BusinessException("Auto endpoint supports geofence events only", "ATTENDANCE_INVALID_EVENT_TYPE");
        }

        GeoFenceMatchResult match = resolveGeofenceMatch(user.getEmployeeId(), request.getLocation());
        AttendanceRecord summary = getOrCreateSummary(user, today);
        String source = request.getSource() != null ? request.getSource() : "AUTO_PWA";
        return recordEvent(user, today, summary, eventType, AttendanceTriggerMode.AUTO, request.getLocation(), source, match, null);
    }

    @Transactional
    public AttendanceActionResponse recordSystemDayClose(
            AttendanceRecord summary,
            LocalDateTime closeTime,
            String remarks) {

        attendanceSessionService.findOpenSession(summary.getEmployeeId(), summary.getAttendanceDate())
                .ifPresent(openSession -> attendanceSessionService.closeSession(
                        openSession,
                        AttendanceEventType.SYSTEM_DAY_CLOSE,
                        AttendanceTriggerMode.SYSTEM,
                        closeTime));

        AttendanceEvent event = AttendanceEvent.builder()
                .employeeId(summary.getEmployeeId())
                .teamId(summary.getTeamId())
                .attendanceDate(summary.getAttendanceDate())
                .attendanceRecordId(summary.getId())
                .eventType(AttendanceEventType.SYSTEM_DAY_CLOSE)
                .eventTime(closeTime)
                .source("SYSTEM")
                .triggerMode(AttendanceTriggerMode.SYSTEM)
                .matchedOfficeLocationId(summary.getMatchedOfficeLocationId())
                .valid(true)
                .remarks(remarks)
                .build();

        AttendanceEvent savedEvent = attendanceEventRepository.save(event);
        refreshSummary(summary);

        summary.setStatus(AttendanceStatus.MISSING_CHECKOUT);
        summary.setCurrentSessionStatus(CurrentSessionStatus.CLOSED);
        summary.setFinalCheckOutTime(closeTime);
        summary.setProcessingStatus(ProcessingStatus.COMPLETED);
        summary.setSource("SYSTEM");

        AttendanceRecord savedSummary = attendanceRecordRepository.save(summary);
        savedEvent.setAttendanceRecordId(savedSummary.getId());
        attendanceEventRepository.save(savedEvent);

        return buildActionResponse(savedSummary, closeTime, savedEvent.getId());
    }

    public boolean hasOpenSession(Long employeeId, LocalDate date) {
        return attendanceSessionService.findOpenSession(employeeId, date).isPresent();
    }

    public boolean isAutoCheckoutEligible(Long employeeId, LocalDate date) {
        return attendanceSessionService.findOpenSession(employeeId, date)
                .map(AttendanceSession::isAutoCheckoutEligible)
                .orElse(false);
    }

    private AttendanceActionResponse recordManualEvent(
            UserPrincipal user,
            LocalDate today,
            AttendanceEventType eventType,
            LocationPayload location,
            String source,
            GeoFenceMatchResult geofenceMatch) {
        AttendanceRecord summary = getOrCreateSummary(user, today);
        String resolvedSource = source != null ? source : "PWA";
        return recordEvent(user, today, summary, eventType, AttendanceTriggerMode.MANUAL, location, resolvedSource, geofenceMatch, null);
    }

    private AttendanceActionResponse recordEvent(
            UserPrincipal user,
            LocalDate today,
            AttendanceRecord summary,
            AttendanceEventType eventType,
            AttendanceTriggerMode triggerMode,
            LocationPayload location,
            String source,
            GeoFenceMatchResult geofenceMatch) {
        AttendanceSession openSession = isCheckOutLikeEvent(eventType)
                ? requireOpenSession(user.getEmployeeId(), today) : null;
        if (isCheckInLikeEvent(eventType)) {
            assertNoOpenSession(user.getEmployeeId(), today);
        }
        return recordEvent(user, today, summary, eventType, triggerMode, location, source, geofenceMatch, openSession);
    }

    private AttendanceActionResponse recordEvent(
            UserPrincipal user,
            LocalDate today,
            AttendanceRecord summary,
            AttendanceEventType eventType,
            AttendanceTriggerMode triggerMode,
            LocationPayload location,
            String source,
            GeoFenceMatchResult geofenceMatch,
            AttendanceSession openSessionForCheckout) {

        LocalDateTime recordedAt = resolveRecordedAt(today, location);
        Long matchedOfficeId = geofenceMatch != null && geofenceMatch.isWithinFence()
                ? geofenceMatch.getOfficeId() : null;
        Double distanceMeters = geofenceMatch != null ? geofenceMatch.getDistanceMeters() : null;

        AttendanceSession linkedSession = null;
        if (isCheckInLikeEvent(eventType)) {
            linkedSession = attendanceSessionService.openSession(
                    user.getEmployeeId(),
                    user.getTeamId(),
                    today,
                    summary.getId(),
                    eventType,
                    triggerMode,
                    recordedAt,
                    geofenceMatch);
        } else if (isCheckOutLikeEvent(eventType)) {
            linkedSession = attendanceSessionService.closeSession(
                    openSessionForCheckout,
                    eventType,
                    triggerMode,
                    recordedAt);
        }

        AttendanceEvent event = AttendanceEvent.builder()
                .employeeId(user.getEmployeeId())
                .teamId(user.getTeamId())
                .attendanceDate(today)
                .attendanceRecordId(summary.getId())
                .attendanceSessionId(linkedSession != null ? linkedSession.getId() : null)
                .eventType(eventType)
                .eventTime(recordedAt)
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracy(location.getAccuracy())
                .geoPoint(GeoPointUtils.createPoint(location.getLatitude(), location.getLongitude()))
                .matchedOfficeLocationId(matchedOfficeId)
                .source(source)
                .triggerMode(triggerMode)
                .valid(true)
                .build();

        AttendanceEvent savedEvent = attendanceEventRepository.save(event);

        List<AttendanceEvent> todayEvents = attendanceEventRepository
                .findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(user.getEmployeeId(), today);
        List<AttendanceSession> todaySessions = attendanceSessionService.findSessionsForDay(user.getEmployeeId(), today);
        DailySummaryService.SummaryApplyResult applyResult =
                dailySummaryService.applySummary(summary, todayEvents, todaySessions);

        summary.setTeamId(user.getTeamId());
        summary.setSource(source);
        if (matchedOfficeId != null) {
            summary.setMatchedOfficeLocationId(matchedOfficeId);
        }
        if (distanceMeters != null && isCheckInLikeEvent(eventType)) {
            summary.setDistanceFromOfficeMeters(distanceMeters);
        }

        if (applyResult.requiresClassification()) {
            summary.setProcessingStatus(ProcessingStatus.CLASSIFICATION_PENDING);
        } else if (isCheckOutLikeEvent(eventType)) {
            summary.setProcessingStatus(ProcessingStatus.COMPLETED);
        }

        AttendanceRecord savedSummary = attendanceRecordRepository.save(summary);
        savedEvent.setAttendanceRecordId(savedSummary.getId());
        attendanceEventRepository.save(savedEvent);

        String outboxAction = mapOutboxAction(eventType);
        if (outboxAction != null && (applyResult.firstCheckInEstablished() || isCheckOutLikeEvent(eventType) || applyResult.requiresClassification())) {
            enqueueClassification(user, savedSummary, savedEvent, outboxAction);
        }

        return buildActionResponse(savedSummary, recordedAt, savedEvent.getId());
    }

    private void refreshSummary(AttendanceRecord summary) {
        dailySummaryService.applySummary(
                summary,
                attendanceEventRepository.findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(
                        summary.getEmployeeId(), summary.getAttendanceDate()),
                attendanceSessionService.findSessionsForDay(summary.getEmployeeId(), summary.getAttendanceDate()));
    }

    private void assertNoOpenSession(Long employeeId, LocalDate today) {
        if (attendanceSessionService.findOpenSession(employeeId, today).isPresent()) {
            throw new BusinessException("An active attendance session already exists", "ATTENDANCE_SESSION_ALREADY_OPEN");
        }
    }

    private AttendanceSession requireOpenSession(Long employeeId, LocalDate today) {
        return attendanceSessionService.findOpenSession(employeeId, today)
                .orElseThrow(() -> new BusinessException("No active attendance session", "ATTENDANCE_NO_OPEN_SESSION"));
    }

    private boolean isCheckInLikeEvent(AttendanceEventType eventType) {
        return eventType == AttendanceEventType.CHECK_IN
                || eventType == AttendanceEventType.MANUAL_CHECK_IN
                || eventType == AttendanceEventType.AUTO_CHECK_IN
                || eventType == AttendanceEventType.WFH_CONFIRMED_CHECK_IN;
    }

    private boolean isCheckOutLikeEvent(AttendanceEventType eventType) {
        return eventType == AttendanceEventType.CHECK_OUT
                || eventType == AttendanceEventType.MANUAL_CHECK_OUT
                || eventType == AttendanceEventType.AUTO_CHECK_OUT
                || eventType == AttendanceEventType.SYSTEM_DAY_CLOSE;
    }

    private GeoFenceMatchResult resolveGeofenceMatch(Long employeeId, LocationPayload location) {
        EmployeeAssignedOfficeDto office = employeeOfficeCacheService.getAssignedOffice(employeeId);
        return assignedOfficeGeofenceService.evaluate(office, location.getLatitude(), location.getLongitude());
    }

    private AttendanceRecord getOrCreateSummary(UserPrincipal user, LocalDate today) {
        return attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                .orElseGet(() -> attendanceRecordRepository.save(AttendanceRecord.builder()
                        .employeeId(user.getEmployeeId())
                        .teamId(user.getTeamId())
                        .attendanceDate(today)
                        .status(AttendanceStatus.RECORDED)
                        .currentSessionStatus(CurrentSessionStatus.NONE)
                        .processingStatus(ProcessingStatus.CLASSIFICATION_PENDING)
                        .build()));
    }

    private void enqueueClassification(
            UserPrincipal user,
            AttendanceRecord summary,
            AttendanceEvent event,
            String action) {
        OutboxEventPayload payload = OutboxEventPayload.builder()
                .employeeId(user.getEmployeeId())
                .teamId(user.getTeamId())
                .attendanceRecordId(summary.getId())
                .attendanceEventId(event.getId())
                .action(action)
                .managerId(user.getManagerId())
                .build();
        OutboxEventType outboxType = "CHECK_IN".equals(action)
                ? OutboxEventType.ATTENDANCE_CHECKED_IN
                : OutboxEventType.ATTENDANCE_CHECKED_OUT;
        outboxService.saveEvent(outboxType, "AttendanceRecord", summary.getId(), payload);
    }

    private String mapOutboxAction(AttendanceEventType eventType) {
        return switch (eventType) {
            case CHECK_IN, MANUAL_CHECK_IN, AUTO_CHECK_IN, WFH_CONFIRMED_CHECK_IN -> "CHECK_IN";
            case CHECK_OUT, MANUAL_CHECK_OUT, AUTO_CHECK_OUT, SYSTEM_DAY_CLOSE -> "CHECK_OUT";
            case ENTERED_GEOFENCE, EXITED_GEOFENCE -> null;
        };
    }

    private LocalDateTime resolveRecordedAt(LocalDate today, LocationPayload location) {
        LocalDateTime now = LocalDateTime.now();
        if (location.getTimestamp() == null) {
            return now;
        }
        LocalDateTime clientTime = location.getTimestamp();
        if (clientTime.isAfter(now.plusMinutes(2)) || clientTime.isBefore(today.atStartOfDay())) {
            return now;
        }
        return clientTime;
    }

    private AttendanceActionResponse buildActionResponse(
            AttendanceRecord saved,
            LocalDateTime recordedAt,
            Long eventId) {
        return AttendanceActionResponse.builder()
                .attendanceId(saved.getId())
                .eventId(eventId)
                .status(saved.getStatus())
                .processingStatus(saved.getProcessingStatus())
                .recordedAt(recordedAt)
                .build();
    }
}
