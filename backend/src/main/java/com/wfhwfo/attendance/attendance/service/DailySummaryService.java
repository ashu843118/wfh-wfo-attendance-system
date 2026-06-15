package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailySummaryService {

    public static final int DEFAULT_REQUIRED_WFO_MINUTES = 180;

    private final AttendancePolicyRepository attendancePolicyRepository;

    public boolean isCheckInEvent(AttendanceEventType eventType) {
        return eventType == AttendanceEventType.CHECK_IN
                || eventType == AttendanceEventType.MANUAL_CHECK_IN
                || eventType == AttendanceEventType.AUTO_CHECK_IN
                || eventType == AttendanceEventType.WFH_CONFIRMED_CHECK_IN;
    }

    public boolean isCheckOutEvent(AttendanceEventType eventType) {
        return eventType == AttendanceEventType.CHECK_OUT
                || eventType == AttendanceEventType.MANUAL_CHECK_OUT
                || eventType == AttendanceEventType.AUTO_CHECK_OUT
                || eventType == AttendanceEventType.SYSTEM_DAY_CLOSE;
    }

    public SummaryApplyResult applySummary(
            AttendanceRecord summary,
            List<AttendanceEvent> events,
            List<AttendanceSession> sessions) {

        List<AttendanceEvent> validEvents = events.stream()
                .filter(AttendanceEvent::isValid)
                .sorted(Comparator.comparing(AttendanceEvent::getEventTime).thenComparing(AttendanceEvent::getId))
                .toList();

        Optional<AttendanceEvent> firstCheckInEvent = validEvents.stream()
                .filter(event -> isCheckInEvent(event.getEventType()))
                .min(Comparator.comparing(AttendanceEvent::getEventTime).thenComparing(AttendanceEvent::getId));

        if (firstCheckInEvent.isEmpty()) {
            clearSummary(summary);
            return SummaryApplyResult.builder()
                    .firstCheckInEstablished(false)
                    .requiresClassification(false)
                    .build();
        }

        boolean firstCheckInEstablished = summary.getFirstCheckInTime() == null;
        AttendanceEvent firstEvent = firstCheckInEvent.get();
        summary.setFirstCheckInTime(firstEvent.getEventTime());
        summary.setCheckInLatitude(firstEvent.getLatitude());
        summary.setCheckInLongitude(firstEvent.getLongitude());
        summary.setCheckInAccuracy(firstEvent.getAccuracy());
        summary.setCheckInGeoPoint(firstEvent.getGeoPoint());
        if (firstEvent.getMatchedOfficeLocationId() != null) {
            summary.setMatchedOfficeLocationId(firstEvent.getMatchedOfficeLocationId());
        }

        Optional<AttendanceEvent> latestCheckOutEvent = validEvents.stream()
                .filter(event -> isCheckOutEvent(event.getEventType()))
                .max(Comparator.comparing(AttendanceEvent::getEventTime).thenComparing(AttendanceEvent::getId));

        latestCheckOutEvent.ifPresent(event -> {
            summary.setFinalCheckOutTime(event.getEventTime());
            summary.setCheckOutLatitude(event.getLatitude());
            summary.setCheckOutLongitude(event.getLongitude());
            summary.setCheckOutAccuracy(event.getAccuracy());
            summary.setCheckOutGeoPoint(event.getGeoPoint());
        });

        boolean hasOpenSession = sessions.stream()
                .anyMatch(session -> session.getStatus() == AttendanceSessionStatus.OPEN);

        AttendanceEvent lastEvent = validEvents.get(validEvents.size() - 1);
        if (lastEvent.getEventType() == AttendanceEventType.SYSTEM_DAY_CLOSE) {
            summary.setStatus(AttendanceStatus.MISSING_CHECKOUT);
            summary.setCurrentSessionStatus(CurrentSessionStatus.CLOSED);
        } else if (hasOpenSession) {
            summary.setStatus(AttendanceStatus.CHECKED_IN);
            summary.setCurrentSessionStatus(CurrentSessionStatus.OPEN);
            Optional<AttendanceSession> openSession = sessions.stream()
                    .filter(session -> session.getStatus() == AttendanceSessionStatus.OPEN)
                    .findFirst();
            openSession.ifPresent(session -> summary.setAttendanceMode(session.getSessionMode()));
        } else if (isCheckOutEvent(lastEvent.getEventType())) {
            summary.setStatus(AttendanceStatus.CHECKED_OUT);
            summary.setCurrentSessionStatus(CurrentSessionStatus.CLOSED);
        } else if (isCheckInEvent(lastEvent.getEventType())) {
            summary.setStatus(AttendanceStatus.CHECKED_IN);
            summary.setCurrentSessionStatus(CurrentSessionStatus.OPEN);
        } else if (latestCheckOutEvent.isPresent()) {
            summary.setStatus(AttendanceStatus.CHECKED_OUT);
            summary.setCurrentSessionStatus(CurrentSessionStatus.CLOSED);
        } else {
            summary.setCurrentSessionStatus(CurrentSessionStatus.NONE);
            if (summary.getFirstCheckInTime() == null) {
                summary.setStatus(AttendanceStatus.RECORDED);
            }
        }

        LocalDateTime eodClose = summary.getFinalCheckOutTime();
        int totalOfficeMinutes = calculateTotalOfficeMinutesFromSessions(sessions, eodClose);
        summary.setTotalOfficeMinutes(totalOfficeMinutes);

        if (!hasOpenSession && !sessions.isEmpty()) {
            int requiredWfoMinutes = resolveRequiredWfoMinutes(summary.getTeamId());
            summary.setAttendanceMode(resolveDailyMode(totalOfficeMinutes, requiredWfoMinutes));
        }

        return SummaryApplyResult.builder()
                .firstCheckInEstablished(firstCheckInEstablished)
                .requiresClassification(firstCheckInEstablished || isCheckInEvent(lastEvent.getEventType()))
                .build();
    }

    /** @deprecated use {@link #applySummary(AttendanceRecord, List, List)} */
    public SummaryApplyResult applySummaryFromEvents(AttendanceRecord summary, List<AttendanceEvent> events) {
        return applySummary(summary, events, List.of());
    }

    public int calculateTotalOfficeMinutesFromSessions(List<AttendanceSession> sessions, LocalDateTime eodCloseTime) {
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
                totalMinutes += java.time.Duration.between(session.getCheckInTime(), end).toMinutes();
            }
        }
        return totalMinutes > 0 ? (int) totalMinutes : 0;
    }

    public AttendanceMode resolveDailyMode(int totalOfficeMinutes, int requiredWfoMinutes) {
        return totalOfficeMinutes >= requiredWfoMinutes ? AttendanceMode.WFO : AttendanceMode.WFH;
    }

    public int resolveRequiredWfoMinutes(Long teamId) {
        if (teamId == null) {
            return DEFAULT_REQUIRED_WFO_MINUTES;
        }
        return attendancePolicyRepository.findByTeamIdAndActiveTrue(teamId)
                .map(AttendancePolicy::getRequiredWfoMinutes)
                .orElse(DEFAULT_REQUIRED_WFO_MINUTES);
    }

    private void clearSummary(AttendanceRecord summary) {
        summary.setFirstCheckInTime(null);
        summary.setFinalCheckOutTime(null);
        summary.setCheckInLatitude(null);
        summary.setCheckInLongitude(null);
        summary.setCheckInAccuracy(null);
        summary.setCheckInGeoPoint(null);
        summary.setCheckOutLatitude(null);
        summary.setCheckOutLongitude(null);
        summary.setCheckOutAccuracy(null);
        summary.setCheckOutGeoPoint(null);
        summary.setTotalOfficeMinutes(null);
        summary.setAttendanceMode(null);
        summary.setCurrentSessionStatus(CurrentSessionStatus.NONE);
        summary.setStatus(AttendanceStatus.RECORDED);
    }

    @Builder
    public record SummaryApplyResult(boolean firstCheckInEstablished, boolean requiresClassification) {
    }
}
