package com.wfhwfo.attendance.attendance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.config.AutoAttendanceProperties;
import com.wfhwfo.attendance.attendance.dto.AutoAttendanceEventRequest;
import com.wfhwfo.attendance.attendance.dto.AttendanceActionResponse;
import com.wfhwfo.attendance.attendance.dto.AttendanceRecordResponse;
import com.wfhwfo.attendance.attendance.dto.AutoTrackingSessionState;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.dto.LocationSignalResponse;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.enums.AutoTrackingStateLabel;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.service.GeofenceService;
import com.wfhwfo.attendance.geofence.service.LocationReliabilityService;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationSignalService {

    private static final Duration STATE_TTL = Duration.ofHours(24);
    private static final String AUTO_SOURCE = "AUTO_PWA";

    private final EmployeeOfficeCacheService employeeOfficeCacheService;
    private final GeofenceService geofenceService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionService attendanceSessionService;
    private final AttendanceWriteService attendanceWriteService;
    private final CacheAdapter cacheAdapter;
    private final ObjectMapper objectMapper;
    private final AutoAttendanceProperties autoAttendanceProperties;
    private final LocationReliabilityService locationReliabilityService;

    @Transactional
    public LocationSignalResponse processLocationSignal(UserPrincipal user, LocationPayload location) {
        LocalDate today = LocalDate.now();
        LocalDateTime signalTime = resolveSignalTime(today, location);

        EmployeeAssignedOfficeDto assignedOffice = employeeOfficeCacheService.getAssignedOffice(user.getEmployeeId());
        GeoFenceMatchResult geofenceMatch = geofenceService.validateForMatch(user.getEmployeeId(), location);
        boolean insideOffice = geofenceMatch.isWithinFence();
        boolean locationReliable = locationReliabilityService.isReliable(location, today);

        log.debug(
                "Location signal received employeeId={} insideOffice={} locationReliable={} accuracy={}",
                user.getEmployeeId(),
                insideOffice,
                locationReliable,
                location.getAccuracy() != null ? Math.round(location.getAccuracy()) : null);

        AutoTrackingSessionState session = loadSession(user.getEmployeeId(), today);
        AttendanceActionResponse actionTaken = null;
        String userMessage = null;

        if (locationReliable) {
            if (insideOffice) {
                if (!Boolean.TRUE.equals(session.getWasInside())) {
                    attendanceWriteService.recordAutoEvent(
                            user, today, buildAutoGeofenceRequest(location, AttendanceEventType.ENTERED_GEOFENCE));
                }
                session.setInsideSince(session.getInsideSince() != null ? session.getInsideSince() : signalTime);
                session.setOutsideSince(null);
            } else {
                if (Boolean.TRUE.equals(session.getWasInside())) {
                    attendanceWriteService.recordAutoEvent(
                            user, today, buildAutoGeofenceRequest(location, AttendanceEventType.EXITED_GEOFENCE));
                }
                session.setOutsideSince(session.getOutsideSince() != null ? session.getOutsideSince() : signalTime);
                session.setInsideSince(null);
            }
            session.setWasInside(insideOffice);
            saveSession(user.getEmployeeId(), today, session);
        }

        AttendanceRecord summary = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                .orElse(null);

        boolean hasOpenSession = attendanceWriteService.hasOpenSession(user.getEmployeeId(), today);
        boolean dayClosed = summary != null && isDayClosed(summary.getStatus());

        Long checkInStableRemaining = null;
        if (locationReliable && !hasOpenSession && !dayClosed && insideOffice && session.getInsideSince() != null) {
            long insideSeconds = Duration.between(session.getInsideSince(), signalTime).getSeconds();
            if (insideSeconds >= autoAttendanceProperties.getCheckInStableSeconds()) {
                actionTaken = mergeAction(actionTaken, attendanceWriteService.recordTrackedEvent(
                        user, today, AttendanceEventType.AUTO_CHECK_IN, AttendanceTriggerMode.AUTO,
                        location, AUTO_SOURCE, geofenceMatch));
                userMessage = "You are inside office geofence. Auto check-in recorded as WFO.";
                session.setInsideSince(null);
                saveSession(user.getEmployeeId(), today, session);
                summary = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                        .orElse(summary);
                hasOpenSession = attendanceWriteService.hasOpenSession(user.getEmployeeId(), today);
            } else {
                checkInStableRemaining = Math.max(0L,
                        autoAttendanceProperties.getCheckInStableSeconds() - insideSeconds);
            }
        }

        if (locationReliable && hasOpenSession && !dayClosed && attendanceWriteService.isAutoCheckoutEligible(user.getEmployeeId(), today)
                && !insideOffice && session.getOutsideSince() != null) {
            long outsideSeconds = Duration.between(session.getOutsideSince(), signalTime).getSeconds();
            if (outsideSeconds >= autoAttendanceProperties.getCheckoutGraceSeconds()) {
                actionTaken = mergeAction(actionTaken, attendanceWriteService.recordTrackedEvent(
                        user, today, AttendanceEventType.AUTO_CHECK_OUT, AttendanceTriggerMode.AUTO,
                        location, AUTO_SOURCE, geofenceMatch));
                session.setOutsideSince(null);
                saveSession(user.getEmployeeId(), today, session);
                summary = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                        .orElse(summary);
                hasOpenSession = attendanceWriteService.hasOpenSession(user.getEmployeeId(), today);
            }
        }

        Long graceRemaining = null;
        summary = attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(user.getEmployeeId(), today)
                .orElse(summary);
        hasOpenSession = attendanceWriteService.hasOpenSession(user.getEmployeeId(), today);
        dayClosed = summary != null && isDayClosed(summary.getStatus());

        if (hasOpenSession && !dayClosed && attendanceWriteService.isAutoCheckoutEligible(user.getEmployeeId(), today)
                && !insideOffice && session.getOutsideSince() != null) {
            graceRemaining = Math.max(0L,
                    autoAttendanceProperties.getCheckoutGraceSeconds()
                            - Duration.between(session.getOutsideSince(), signalTime).getSeconds());
        }

        boolean requiresWfhConfirmation = !hasOpenSession && !dayClosed && !insideOffice
                && !Boolean.TRUE.equals(session.getWfhPromptDismissed());

        AutoTrackingStateLabel trackingState = resolveTrackingState(
                summary, insideOffice, locationReliable, graceRemaining, checkInStableRemaining,
                requiresWfhConfirmation, session.getWfhPromptDismissed(), hasOpenSession);

        return LocationSignalResponse.builder()
                .trackingState(trackingState)
                .insideOffice(insideOffice)
                .locationReliable(locationReliable)
                .assignedOfficeName(assignedOffice.getOfficeName())
                .matchedOfficeName(geofenceMatch.isWithinFence() ? assignedOffice.getOfficeName() : null)
                .distanceFromOfficeMeters(geofenceMatch.getDistanceMeters())
                .checkInStableSecondsRemaining(checkInStableRemaining)
                .graceSecondsRemaining(graceRemaining)
                .requiresWfhConfirmation(requiresWfhConfirmation)
                .userMessage(userMessage)
                .todaySummary(summary != null ? toRecordResponse(summary) : null)
                .actionTaken(actionTaken)
                .build();
    }

    public void dismissWfhPrompt(Long employeeId, LocalDate date) {
        AutoTrackingSessionState session = loadSession(employeeId, date);
        session.setWfhPromptDismissed(true);
        saveSession(employeeId, date, session);
    }

    private boolean isDayClosed(AttendanceStatus status) {
        return status == AttendanceStatus.SYSTEM_CLOSED
                || status == AttendanceStatus.MISSING_CHECKOUT;
    }

    private boolean isTerminalStatus(AttendanceStatus status) {
        return status == AttendanceStatus.CHECKED_OUT
                || isDayClosed(status);
    }

    private AutoTrackingStateLabel resolveTrackingState(
            AttendanceRecord summary,
            boolean insideOffice,
            boolean locationReliable,
            Long graceRemaining,
            Long checkInStableRemaining,
            boolean requiresWfhConfirmation,
            Boolean wfhPromptDismissed,
            boolean hasOpenSession) {
        if (summary != null && summary.getStatus() == AttendanceStatus.SYSTEM_CLOSED) {
            return AutoTrackingStateLabel.SYSTEM_CLOSED;
        }
        if (summary != null && summary.getStatus() == AttendanceStatus.MISSING_CHECKOUT) {
            return AutoTrackingStateLabel.MISSING_CHECKOUT;
        }
        if (summary != null && summary.getStatus() == AttendanceStatus.CHECKED_OUT && !hasOpenSession) {
            return AutoTrackingStateLabel.CHECKED_OUT;
        }
        if (hasOpenSession && summary != null) {
            AttendanceMode openSessionMode = attendanceSessionService.findOpenSession(
                            summary.getEmployeeId(), summary.getAttendanceDate())
                    .map(AttendanceSession::getSessionMode)
                    .orElse(summary.getAttendanceMode());
            if (openSessionMode == AttendanceMode.WFH) {
                return AutoTrackingStateLabel.CHECKED_IN_WFH;
            }
            if (openSessionMode == AttendanceMode.WFO) {
                if (!insideOffice && graceRemaining != null && graceRemaining > 0) {
                    return AutoTrackingStateLabel.AUTO_CHECKOUT_PENDING;
                }
                return AutoTrackingStateLabel.AUTO_CHECKOUT_MONITORING_ACTIVE;
            }
        }
        if (summary != null && summary.getFirstCheckInTime() != null
                && summary.getStatus() == AttendanceStatus.CHECKED_IN) {
            if (summary.getProcessingStatus() == ProcessingStatus.COMPLETED) {
                if (summary.getAttendanceMode() == AttendanceMode.WFH) {
                    return AutoTrackingStateLabel.CHECKED_IN_WFH;
                }
                if (summary.getAttendanceMode() == AttendanceMode.WFO) {
                    return AutoTrackingStateLabel.CHECKED_IN_WFO;
                }
            }
            if (!insideOffice && graceRemaining != null && graceRemaining > 0) {
                return AutoTrackingStateLabel.AUTO_CHECKOUT_PENDING;
            }
            return AutoTrackingStateLabel.AUTO_CHECKED_IN;
        }
        if (requiresWfhConfirmation) {
            return AutoTrackingStateLabel.WFH_CONFIRMATION_REQUIRED;
        }
        if (insideOffice && !locationReliable) {
            return AutoTrackingStateLabel.POOR_LOCATION_ACCURACY;
        }
        if (!insideOffice && !requiresWfhConfirmation && Boolean.TRUE.equals(wfhPromptDismissed)) {
            return AutoTrackingStateLabel.OUTSIDE_OFFICE;
        }
        if (insideOffice && checkInStableRemaining != null && checkInStableRemaining > 0) {
            return AutoTrackingStateLabel.AUTO_CHECKIN_PENDING;
        }
        if (insideOffice) {
            return AutoTrackingStateLabel.INSIDE_OFFICE;
        }
        return AutoTrackingStateLabel.NOT_CHECKED_IN;
    }

    private AttendanceActionResponse mergeAction(
            AttendanceActionResponse existing,
            AttendanceActionResponse next) {
        return next != null ? next : existing;
    }

    private LocalDateTime resolveSignalTime(LocalDate today, LocationPayload location) {
        LocalDateTime now = LocalDateTime.now();
        if (location.getTimestamp() == null) {
            return now;
        }
        if (location.getTimestamp().isAfter(now.plusMinutes(2))
                || location.getTimestamp().isBefore(today.atStartOfDay())) {
            return now;
        }
        return location.getTimestamp();
    }

    private AutoTrackingSessionState loadSession(Long employeeId, LocalDate date) {
        String key = sessionKey(employeeId, date);
        return cacheAdapter.get(key)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, AutoTrackingSessionState.class);
                    } catch (JsonProcessingException ex) {
                        log.warn("Failed to parse auto tracking session for employee {}", employeeId);
                        return AutoTrackingSessionState.builder().build();
                    }
                })
                .orElseGet(() -> AutoTrackingSessionState.builder().build());
    }

    private void saveSession(Long employeeId, LocalDate date, AutoTrackingSessionState state) {
        try {
            cacheAdapter.put(sessionKey(employeeId, date), objectMapper.writeValueAsString(state), STATE_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to persist auto tracking session", ex);
        }
    }

    private String sessionKey(Long employeeId, LocalDate date) {
        return "attendance:auto:session:" + employeeId + ":" + date;
    }

    private AutoAttendanceEventRequest buildAutoGeofenceRequest(LocationPayload location, AttendanceEventType eventType) {
        return AutoAttendanceEventRequest.builder()
                .eventType(eventType)
                .location(location)
                .source(AUTO_SOURCE)
                .build();
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        AttendanceMode currentSessionMode = null;
        if (record.getCurrentSessionStatus() == CurrentSessionStatus.OPEN) {
            currentSessionMode = attendanceSessionService.findOpenSession(
                            record.getEmployeeId(), record.getAttendanceDate())
                    .map(AttendanceSession::getSessionMode)
                    .orElse(null);
        }
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .attendanceDate(record.getAttendanceDate())
                .checkInTime(record.getFirstCheckInTime())
                .checkOutTime(record.getFinalCheckOutTime())
                .attendanceMode(record.getAttendanceMode())
                .currentSessionMode(currentSessionMode)
                .status(record.getStatus())
                .processingStatus(record.getProcessingStatus())
                .late(record.getLate())
                .matchedOfficeLocationId(record.getMatchedOfficeLocationId())
                .distanceFromOfficeMeters(record.getDistanceFromOfficeMeters())
                .totalOfficeMinutes(record.getTotalOfficeMinutes())
                .currentSessionStatus(record.getCurrentSessionStatus())
                .source(record.getSource())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
