package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceSessionRepository;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;

    public Optional<AttendanceSession> findOpenSession(Long employeeId, LocalDate date) {
        return attendanceSessionRepository.findByEmployeeIdAndAttendanceDateAndStatus(
                employeeId, date, AttendanceSessionStatus.OPEN);
    }

    public List<AttendanceSession> findSessionsForDay(Long employeeId, LocalDate date) {
        return attendanceSessionRepository.findByEmployeeIdAndAttendanceDateOrderByCheckInTimeAscIdAsc(
                employeeId, date);
    }

    public AttendanceSession openSession(
            Long employeeId,
            Long teamId,
            LocalDate date,
            Long attendanceRecordId,
            AttendanceEventType checkInEventType,
            AttendanceTriggerMode triggerMode,
            LocalDateTime checkInTime,
            GeoFenceMatchResult geofenceMatch) {

        if (findOpenSession(employeeId, date).isPresent()) {
            throw new BusinessException("An active attendance session already exists", "ATTENDANCE_SESSION_ALREADY_OPEN");
        }

        AttendanceMode sessionMode = resolveSessionMode(checkInEventType, geofenceMatch);
        boolean autoCheckoutEligible = checkInEventType == AttendanceEventType.AUTO_CHECK_IN;

        Long matchedOfficeId = geofenceMatch != null && geofenceMatch.isWithinFence()
                ? geofenceMatch.getOfficeId() : null;

        AttendanceSession session = AttendanceSession.builder()
                .employeeId(employeeId)
                .teamId(teamId)
                .attendanceDate(date)
                .attendanceRecordId(attendanceRecordId)
                .sessionMode(sessionMode)
                .checkInEventType(checkInEventType)
                .checkInTime(checkInTime)
                .checkInTriggerMode(triggerMode)
                .autoCheckoutEligible(autoCheckoutEligible)
                .status(AttendanceSessionStatus.OPEN)
                .matchedOfficeLocationId(matchedOfficeId)
                .build();

        return attendanceSessionRepository.save(session);
    }

    public AttendanceSession closeSession(
            AttendanceSession session,
            AttendanceEventType checkOutEventType,
            AttendanceTriggerMode triggerMode,
            LocalDateTime checkOutTime) {

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new BusinessException("No open attendance session to close", "ATTENDANCE_NO_OPEN_SESSION");
        }

        session.setCheckOutTime(checkOutTime);
        session.setCheckOutEventType(checkOutEventType);
        session.setStatus(checkOutEventType == AttendanceEventType.SYSTEM_DAY_CLOSE
                ? AttendanceSessionStatus.SYSTEM_CLOSED
                : AttendanceSessionStatus.CLOSED);
        return attendanceSessionRepository.save(session);
    }

    public AttendanceMode resolveSessionMode(AttendanceEventType checkInEventType, GeoFenceMatchResult geofenceMatch) {
        if (checkInEventType == AttendanceEventType.WFH_CONFIRMED_CHECK_IN) {
            return AttendanceMode.WFH;
        }
        if (checkInEventType == AttendanceEventType.AUTO_CHECK_IN) {
            return AttendanceMode.WFO;
        }
        if (geofenceMatch != null && geofenceMatch.isWithinFence()) {
            return AttendanceMode.WFO;
        }
        return AttendanceMode.WFH;
    }

    public int calculateTotalOfficeMinutes(List<AttendanceSession> sessions, LocalDateTime eodCloseTime) {
        long totalMinutes = 0;
        for (AttendanceSession session : sessions) {
            if (session.getSessionMode() != AttendanceMode.WFO) {
                continue;
            }
            LocalDateTime end = session.getCheckOutTime();
            if (end == null && session.getStatus() == AttendanceSessionStatus.OPEN && eodCloseTime != null) {
                end = eodCloseTime;
            }
            if (end != null && end.isAfter(session.getCheckInTime())) {
                totalMinutes += Duration.between(session.getCheckInTime(), end).toMinutes();
            }
        }
        return totalMinutes > 0 ? (int) totalMinutes : 0;
    }

    public AttendanceMode resolveDailyMode(int totalOfficeMinutes, int requiredWfoMinutes) {
        return totalOfficeMinutes >= requiredWfoMinutes ? AttendanceMode.WFO : AttendanceMode.WFH;
    }
}
