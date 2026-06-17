package com.wfhwfo.attendance.outbox.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.attendance.service.AttendanceSessionService;
import com.wfhwfo.attendance.attendance.service.DailySummaryService;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.service.GeofenceService;
import com.wfhwfo.attendance.outbox.dto.OutboxEventPayload;
import com.wfhwfo.attendance.outbox.entity.OutboxEvent;
import com.wfhwfo.attendance.outbox.service.OutboxService;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class AttendanceClassificationProcessor implements OutboxEventProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            OutboxEventType.ATTENDANCE_CHECKED_IN.name(),
            OutboxEventType.ATTENDANCE_CHECKED_OUT.name()
    );

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceSessionService attendanceSessionService;
    private final DailySummaryService dailySummaryService;
    private final GeofenceService geofenceService;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    @Transactional
    public void process(OutboxEvent event) throws Exception {
        OutboxEventPayload payload = objectMapper.readValue(event.getPayload(), OutboxEventPayload.class);
        AttendanceRecord record = attendanceRecordRepository.findById(payload.getAttendanceRecordId())
                .orElseThrow(() -> new IllegalStateException("Attendance record not found: " + payload.getAttendanceRecordId()));

        if (record.getProcessingStatus() == ProcessingStatus.COMPLETED && "CHECK_OUT".equals(payload.getAction())) {
            return;
        }

        List<AttendanceSession> sessions = attendanceSessionService.findSessionsForDay(
                record.getEmployeeId(), record.getAttendanceDate());

        if ("CHECK_IN".equals(payload.getAction())) {
            applyOpenSessionMode(record, payload.getAttendanceEventId(), sessions);
        } else {
            applyFinalDailyMode(record, sessions);
        }

        calculateLate(record, payload.getTeamId());
        record.setProcessingStatus(ProcessingStatus.COMPLETED);
        attendanceRecordRepository.save(record);

        OutboxEventPayload completedPayload = OutboxEventPayload.builder()
                .employeeId(record.getEmployeeId())
                .teamId(record.getTeamId())
                .attendanceRecordId(record.getId())
                .action(payload.getAction())
                .managerId(payload.getManagerId())
                .build();
        outboxService.saveEvent(
                OutboxEventType.ATTENDANCE_CLASSIFICATION_COMPLETED,
                "AttendanceRecord",
                record.getId(),
                completedPayload
        );
    }

    private void applyOpenSessionMode(AttendanceRecord record, Long attendanceEventId, List<AttendanceSession> sessions) {
        sessions.stream()
                .filter(session -> session.getCheckOutTime() == null)
                .findFirst()
                .ifPresent(session -> record.setAttendanceMode(session.getSessionMode()));

        if (record.getAttendanceMode() != null) {
            return;
        }

        if (attendanceEventId != null) {
            AttendanceEvent checkInEvent = attendanceEventRepository.findById(attendanceEventId).orElse(null);
            if (checkInEvent != null) {
                if (checkInEvent.getEventType() == AttendanceEventType.WFH_CONFIRMED_CHECK_IN) {
                    record.setAttendanceMode(AttendanceMode.WFH);
                    return;
                }
                if (checkInEvent.getEventType() == AttendanceEventType.AUTO_CHECK_IN) {
                    record.setAttendanceMode(AttendanceMode.WFO);
                    return;
                }
                if (checkInEvent.getLatitude() != null) {
                    applyDistanceFromAssignedOffice(record, checkInEvent.getLatitude(), checkInEvent.getLongitude());
                    return;
                }
            }
        }

        record.setAttendanceMode(AttendanceMode.WFH);
    }

    private void applyFinalDailyMode(AttendanceRecord record, List<AttendanceSession> sessions) {
        int totalOfficeMinutes = dailySummaryService.calculateTotalOfficeMinutesFromSessions(
                sessions, record.getFinalCheckOutTime());
        int requiredWfoMinutes = dailySummaryService.resolveRequiredWfoMinutes(record.getTeamId());
        record.setAttendanceMode(dailySummaryService.resolveDailyMode(totalOfficeMinutes, requiredWfoMinutes));
        record.setTotalOfficeMinutes(totalOfficeMinutes);
    }

    private void applyDistanceFromAssignedOffice(AttendanceRecord record, double latitude, double longitude) {
        GeoFenceMatchResult match = geofenceService.validateForMatch(
                record.getEmployeeId(),
                LocationPayload.builder().latitude(latitude).longitude(longitude).build());
        if (match.isWithinFence()) {
            record.setAttendanceMode(AttendanceMode.WFO);
            record.setMatchedOfficeLocationId(match.getOfficeId());
            record.setDistanceFromOfficeMeters(match.getDistanceMeters());
        } else {
            record.setAttendanceMode(AttendanceMode.WFH);
            record.setDistanceFromOfficeMeters(match.getDistanceMeters());
        }
    }

    private void calculateLate(AttendanceRecord record, Long teamId) {
        if (record.getFirstCheckInTime() == null || teamId == null) {
            record.setLate(false);
            return;
        }

        AttendancePolicy policy = attendancePolicyRepository.findByTeamIdAndActiveTrue(teamId).orElse(null);
        if (policy == null) {
            record.setLate(false);
            return;
        }

        LocalTime checkInTime = record.getFirstCheckInTime().toLocalTime();
        LocalTime threshold = policy.getStandardCheckInTime()
                .plusMinutes(policy.getLateThresholdMinutes());
        record.setLate(checkInTime.isAfter(threshold));
    }
}
