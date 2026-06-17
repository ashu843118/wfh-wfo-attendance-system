package com.wfhwfo.attendance.attendance.service;

import com.wfhwfo.attendance.attendance.dto.CheckInRequest;
import com.wfhwfo.attendance.attendance.dto.CheckOutRequest;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.enums.OutboxEventType;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.service.AssignedOfficeGeofenceService;
import com.wfhwfo.attendance.geofence.service.LocationReliabilityService;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import com.wfhwfo.attendance.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceWriteServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private AttendanceEventRepository attendanceEventRepository;
    @Mock
    private AttendanceSessionService attendanceSessionService;
    @Mock
    private DailySummaryService dailySummaryService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private EmployeeOfficeCacheService employeeOfficeCacheService;
    @Mock
    private AssignedOfficeGeofenceService assignedOfficeGeofenceService;
    @Mock
    private LocationReliabilityService locationReliabilityService;

    @InjectMocks
    private AttendanceWriteService attendanceWriteService;

    private final EmployeeAssignedOfficeDto assignedOffice = EmployeeAssignedOfficeDto.builder()
            .officeLocationId(1L)
            .officeName("Pune Tech Park")
            .latitude(18.5912)
            .longitude(73.7389)
            .radiusMeters(800)
            .active(true)
            .build();

    private final UserPrincipal user = UserPrincipal.builder()
            .employeeId(1L)
            .email("employee@demo.com")
            .name("Employee")
            .role(Role.EMPLOYEE)
            .teamId(1L)
            .managerId(6L)
            .build();

    @Test
    void checkInCreatesEventSummaryAndOutboxEvent() {
        LocalDate today = LocalDate.now();
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder().officeId(1L).withinFence(true).distanceMeters(10.0).build());
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(true);
        when(attendanceSessionService.findOpenSession(1L, today)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(inv -> {
            AttendanceRecord record = inv.getArgument(0);
            record.setId(100L);
            return record;
        });
        when(attendanceSessionService.openSession(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(AttendanceSession.builder().id(10L).sessionMode(AttendanceMode.WFO).build());
        when(attendanceEventRepository.save(any(AttendanceEvent.class))).thenAnswer(inv -> {
            AttendanceEvent event = inv.getArgument(0);
            event.setId(200L);
            return event;
        });
        when(attendanceEventRepository.findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(1L, today))
                .thenReturn(List.of());
        when(attendanceSessionService.findSessionsForDay(1L, today)).thenReturn(List.of());
        when(dailySummaryService.applySummary(any(), any(), any()))
                .thenReturn(new DailySummaryService.SummaryApplyResult(true, true));

        CheckInRequest request = CheckInRequest.builder()
                .location(LocationPayload.builder()
                        .latitude(18.5912)
                        .longitude(73.7389)
                        .accuracy(10.0)
                        .build())
                .source("PWA")
                .build();

        var response = attendanceWriteService.checkIn(user, today, request);

        assertThat(response.getAttendanceId()).isEqualTo(100L);
        assertThat(response.getEventId()).isEqualTo(200L);
        assertThat(response.getProcessingStatus()).isEqualTo(ProcessingStatus.CLASSIFICATION_PENDING);

        verify(outboxService).saveEvent(
                eq(OutboxEventType.ATTENDANCE_CHECKED_IN),
                eq("AttendanceRecord"),
                eq(100L),
                any());
    }

    @Test
    void checkOutRequiresOpenSession() {
        LocalDate today = LocalDate.now();
        when(attendanceSessionService.findOpenSession(1L, today)).thenReturn(Optional.empty());

        CheckOutRequest request = CheckOutRequest.builder()
                .location(LocationPayload.builder().latitude(1.0).longitude(1.0).accuracy(1.0).build())
                .build();

        assertThatThrownBy(() -> attendanceWriteService.checkOut(user, today, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active attendance session");
    }

    @Test
    void allowsReCheckInAfterPreviousSessionClosed() {
        LocalDate today = LocalDate.now();
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder().withinFence(false).distanceMeters(100.0).build());
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(100L)
                .employeeId(1L)
                .attendanceDate(today)
                .firstCheckInTime(LocalDateTime.now().minusHours(4))
                .status(AttendanceStatus.CHECKED_OUT)
                .build();
        when(attendanceSessionService.findOpenSession(1L, today)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today))
                .thenReturn(Optional.of(existing));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attendanceSessionService.openSession(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(AttendanceSession.builder().id(11L).sessionMode(AttendanceMode.WFH).build());
        when(attendanceEventRepository.save(any(AttendanceEvent.class))).thenAnswer(inv -> {
            AttendanceEvent event = inv.getArgument(0);
            event.setId(201L);
            return event;
        });
        when(attendanceEventRepository.findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(1L, today))
                .thenReturn(List.of());
        when(attendanceSessionService.findSessionsForDay(1L, today)).thenReturn(List.of());
        when(dailySummaryService.applySummary(any(), any(), any()))
                .thenReturn(new DailySummaryService.SummaryApplyResult(false, true));

        CheckInRequest request = CheckInRequest.builder()
                .location(LocationPayload.builder().latitude(1.0).longitude(1.0).accuracy(1.0).build())
                .build();

        var response = attendanceWriteService.checkIn(user, today, request);

        assertThat(response.getEventId()).isEqualTo(201L);
        verify(attendanceSessionService).openSession(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsOfficeCheckInWhenLocationAccuracyIsPoor() {
        LocalDate today = LocalDate.now();
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder().officeId(1L).withinFence(true).distanceMeters(10.0).build());
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(false);
        when(attendanceSessionService.findOpenSession(1L, today)).thenReturn(Optional.empty());

        CheckInRequest request = CheckInRequest.builder()
                .location(LocationPayload.builder()
                        .latitude(18.5912)
                        .longitude(73.7389)
                        .accuracy(250.0)
                        .build())
                .source("PWA")
                .build();

        assertThatThrownBy(() -> attendanceWriteService.checkIn(user, today, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOCATION_ACCURACY_POOR");
    }
}
