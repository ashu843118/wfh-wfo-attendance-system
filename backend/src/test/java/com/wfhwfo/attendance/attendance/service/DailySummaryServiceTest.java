package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DailySummaryServiceTest {

    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;

    private DailySummaryService dailySummaryService;

    @BeforeEach
    void setUp() {
        dailySummaryService = new DailySummaryService(attendancePolicyRepository);
    }

    @Test
    void derivesFirstCheckInIncludingWfhConfirmed() {
        LocalDate date = LocalDate.of(2026, 6, 14);
        AttendanceRecord summary = AttendanceRecord.builder()
                .employeeId(1L)
                .teamId(1L)
                .attendanceDate(date)
                .status(AttendanceStatus.RECORDED)
                .build();

        List<AttendanceEvent> events = List.of(
                event(1L, date, AttendanceEventType.WFH_CONFIRMED_CHECK_IN, LocalDateTime.of(2026, 6, 14, 9, 15)),
                event(2L, date, AttendanceEventType.MANUAL_CHECK_OUT, LocalDateTime.of(2026, 6, 14, 18, 0))
        );

        dailySummaryService.applySummary(summary, events, List.of(
                wfhSession(date, LocalDateTime.of(2026, 6, 14, 9, 15), LocalDateTime.of(2026, 6, 14, 18, 0))));

        assertThat(summary.getFirstCheckInTime()).isEqualTo(LocalDateTime.of(2026, 6, 14, 9, 15));
        assertThat(summary.getStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(summary.getAttendanceMode()).isEqualTo(AttendanceMode.WFH);
    }

    @Test
    void derivesFirstCheckInAndFinalCheckoutFromEvents() {
        LocalDate date = LocalDate.of(2026, 6, 14);
        AttendanceRecord summary = AttendanceRecord.builder()
                .employeeId(1L)
                .teamId(1L)
                .attendanceDate(date)
                .status(AttendanceStatus.RECORDED)
                .build();

        List<AttendanceEvent> events = List.of(
                event(1L, date, AttendanceEventType.AUTO_CHECK_IN, LocalDateTime.of(2026, 6, 14, 9, 5)),
                event(4L, date, AttendanceEventType.AUTO_CHECK_OUT, LocalDateTime.of(2026, 6, 14, 18, 10))
        );

        dailySummaryService.applySummary(summary, events, List.of(
                wfoSession(date, LocalDateTime.of(2026, 6, 14, 9, 5), LocalDateTime.of(2026, 6, 14, 18, 10))));

        assertThat(summary.getFirstCheckInTime()).isEqualTo(LocalDateTime.of(2026, 6, 14, 9, 5));
        assertThat(summary.getFinalCheckOutTime()).isEqualTo(LocalDateTime.of(2026, 6, 14, 18, 10));
        assertThat(summary.getStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(summary.getTotalOfficeMinutes()).isEqualTo(545);
    }

    @Test
    void finalDailyModeStaysWfoWhenOfficeMinutesMeetThresholdDespiteLaterWfhSession() {
        LocalDate date = LocalDate.of(2026, 6, 14);
        AttendanceRecord summary = AttendanceRecord.builder()
                .employeeId(1L)
                .teamId(1L)
                .attendanceDate(date)
                .build();

        List<AttendanceEvent> events = List.of(
                event(1L, date, AttendanceEventType.AUTO_CHECK_IN, LocalDateTime.of(2026, 6, 14, 9, 30)),
                event(2L, date, AttendanceEventType.AUTO_CHECK_OUT, LocalDateTime.of(2026, 6, 14, 12, 30)),
                event(3L, date, AttendanceEventType.MANUAL_CHECK_IN, LocalDateTime.of(2026, 6, 14, 16, 0)),
                event(4L, date, AttendanceEventType.MANUAL_CHECK_OUT, LocalDateTime.of(2026, 6, 14, 20, 0))
        );

        dailySummaryService.applySummary(summary, events, List.of(
                wfoSession(date, LocalDateTime.of(2026, 6, 14, 9, 30), LocalDateTime.of(2026, 6, 14, 12, 30)),
                wfhSession(date, LocalDateTime.of(2026, 6, 14, 16, 0), LocalDateTime.of(2026, 6, 14, 20, 0))));

        assertThat(summary.getTotalOfficeMinutes()).isEqualTo(180);
        assertThat(summary.getAttendanceMode()).isEqualTo(AttendanceMode.WFO);
    }

    @Test
    void systemDayCloseSetsMissingCheckoutStatus() {
        LocalDate date = LocalDate.of(2026, 6, 14);
        AttendanceRecord summary = AttendanceRecord.builder()
                .employeeId(1L)
                .teamId(1L)
                .attendanceDate(date)
                .build();

        List<AttendanceEvent> events = List.of(
                event(1L, date, AttendanceEventType.MANUAL_CHECK_IN, LocalDateTime.of(2026, 6, 14, 9, 0)),
                event(2L, date, AttendanceEventType.SYSTEM_DAY_CLOSE, LocalDateTime.of(2026, 6, 14, 23, 59, 59))
        );

        dailySummaryService.applySummary(summary, events, List.of(
                openWfhSession(date, LocalDateTime.of(2026, 6, 14, 9, 0))));

        assertThat(summary.getStatus()).isEqualTo(AttendanceStatus.MISSING_CHECKOUT);
        assertThat(summary.getFinalCheckOutTime()).isEqualTo(LocalDateTime.of(2026, 6, 14, 23, 59, 59));
    }

    @Test
    void geofenceOnlyEventAfterCheckoutKeepsCheckedOutStatus() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        AttendanceRecord summary = AttendanceRecord.builder()
                .employeeId(1L)
                .teamId(1L)
                .attendanceDate(date)
                .build();

        List<AttendanceEvent> events = List.of(
                event(1L, date, AttendanceEventType.AUTO_CHECK_IN, LocalDateTime.of(2026, 6, 15, 9, 30)),
                event(2L, date, AttendanceEventType.AUTO_CHECK_OUT, LocalDateTime.of(2026, 6, 15, 12, 0)),
                event(3L, date, AttendanceEventType.ENTERED_GEOFENCE, LocalDateTime.of(2026, 6, 15, 14, 0))
        );

        dailySummaryService.applySummary(summary, events, List.of(
                wfoSession(date, LocalDateTime.of(2026, 6, 15, 9, 30), LocalDateTime.of(2026, 6, 15, 12, 0))));

        assertThat(summary.getStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(summary.getCurrentSessionStatus()).isEqualTo(CurrentSessionStatus.CLOSED);
    }

    private AttendanceEvent event(long id, LocalDate date, AttendanceEventType type, LocalDateTime time) {
        return AttendanceEvent.builder()
                .id(id)
                .employeeId(1L)
                .attendanceDate(date)
                .eventType(type)
                .eventTime(time)
                .source("TEST")
                .triggerMode(AttendanceTriggerMode.MANUAL)
                .valid(true)
                .build();
    }

    private AttendanceSession wfoSession(LocalDate date, LocalDateTime checkIn, LocalDateTime checkOut) {
        return AttendanceSession.builder()
                .employeeId(1L)
                .attendanceDate(date)
                .sessionMode(AttendanceMode.WFO)
                .checkInEventType(AttendanceEventType.AUTO_CHECK_IN)
                .checkInTime(checkIn)
                .checkInTriggerMode(AttendanceTriggerMode.AUTO)
                .checkOutTime(checkOut)
                .status(AttendanceSessionStatus.CLOSED)
                .autoCheckoutEligible(true)
                .build();
    }

    private AttendanceSession wfhSession(LocalDate date, LocalDateTime checkIn, LocalDateTime checkOut) {
        return AttendanceSession.builder()
                .employeeId(1L)
                .attendanceDate(date)
                .sessionMode(AttendanceMode.WFH)
                .checkInEventType(AttendanceEventType.WFH_CONFIRMED_CHECK_IN)
                .checkInTime(checkIn)
                .checkInTriggerMode(AttendanceTriggerMode.MANUAL)
                .checkOutTime(checkOut)
                .status(AttendanceSessionStatus.CLOSED)
                .autoCheckoutEligible(false)
                .build();
    }

    private AttendanceSession openWfhSession(LocalDate date, LocalDateTime checkIn) {
        return AttendanceSession.builder()
                .employeeId(1L)
                .attendanceDate(date)
                .sessionMode(AttendanceMode.WFH)
                .checkInEventType(AttendanceEventType.MANUAL_CHECK_IN)
                .checkInTime(checkIn)
                .checkInTriggerMode(AttendanceTriggerMode.MANUAL)
                .status(AttendanceSessionStatus.OPEN)
                .autoCheckoutEligible(false)
                .build();
    }
}
