package com.wfhwfo.attendance.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wfhwfo.attendance.attendance.config.AutoAttendanceProperties;
import com.wfhwfo.attendance.attendance.dto.AutoTrackingSessionState;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.attendance.dto.LocationSignalResponse;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.enums.AutoTrackingStateLabel;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.service.AssignedOfficeGeofenceService;
import com.wfhwfo.attendance.geofence.service.LocationReliabilityService;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationSignalServiceTest {

    @Mock
    private EmployeeOfficeCacheService employeeOfficeCacheService;
    @Mock
    private AssignedOfficeGeofenceService assignedOfficeGeofenceService;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private AttendanceSessionService attendanceSessionService;
    @Mock
    private AttendanceWriteService attendanceWriteService;
    @Mock
    private CacheAdapter cacheAdapter;
    @Mock
    private LocationReliabilityService locationReliabilityService;

    private LocationSignalService locationSignalService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UserPrincipal user = UserPrincipal.builder()
            .employeeId(1L)
            .email("employee@demo.com")
            .role(Role.EMPLOYEE)
            .teamId(1L)
            .managerId(6L)
            .build();

    private final EmployeeAssignedOfficeDto assignedOffice = EmployeeAssignedOfficeDto.builder()
            .officeLocationId(1L)
            .officeName("Pune Tech Park")
            .latitude(18.5912)
            .longitude(73.7389)
            .radiusMeters(800)
            .active(true)
            .build();

    @BeforeEach
    void setUp() {
        AutoAttendanceProperties properties = new AutoAttendanceProperties();
        properties.setCheckInStableSeconds(15);
        properties.setCheckoutGraceSeconds(60);
        locationSignalService = new LocationSignalService(
                employeeOfficeCacheService,
                assignedOfficeGeofenceService,
                attendanceRecordRepository,
                attendanceSessionService,
                attendanceWriteService,
                cacheAdapter,
                objectMapper,
                properties,
                locationReliabilityService);
    }

    @Test
    void autoCheckInWhenInsideAssignedOfficeAfterStableDuration() throws Exception {
        LocalDate today = LocalDate.now();
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(true);
        LocalDateTime signalTime = LocalDateTime.now();
        AutoTrackingSessionState existingSession = AutoTrackingSessionState.builder()
                .wasInside(true)
                .insideSince(signalTime.minusSeconds(20))
                .build();
        when(cacheAdapter.get(anyString())).thenReturn(Optional.of(objectMapper.writeValueAsString(existingSession)));
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder()
                        .officeId(1L)
                        .officeName("Pune Tech Park")
                        .distanceMeters(50.0)
                        .withinFence(true)
                        .build());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());

        LocationSignalResponse response = locationSignalService.processLocationSignal(user, LocationPayload.builder()
                .latitude(18.5912)
                .longitude(73.7389)
                .accuracy(10.0)
                .timestamp(signalTime)
                .build());

        verify(attendanceWriteService).recordTrackedEvent(
                eq(user),
                eq(today),
                eq(AttendanceEventType.AUTO_CHECK_IN),
                eq(AttendanceTriggerMode.AUTO),
                org.mockito.ArgumentMatchers.any(LocationPayload.class),
                eq("AUTO_PWA"),
                any(GeoFenceMatchResult.class));
        assertThat(response.isInsideOffice()).isTrue();
        assertThat(response.getUserMessage()).contains("Auto check-in recorded as WFO");
    }

    @Test
    void pendingAutoCheckInWhenInsideButNotStableYet() {
        LocalDate today = LocalDate.now();
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(true);
        when(cacheAdapter.get(anyString())).thenReturn(Optional.empty());
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder()
                        .officeId(1L)
                        .officeName("Pune Tech Park")
                        .distanceMeters(50.0)
                        .withinFence(true)
                        .build());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());

        LocationSignalResponse response = locationSignalService.processLocationSignal(user, LocationPayload.builder()
                .latitude(18.5912)
                .longitude(73.7389)
                .accuracy(10.0)
                .timestamp(LocalDateTime.now())
                .build());

        assertThat(response.getTrackingState()).isEqualTo(AutoTrackingStateLabel.AUTO_CHECKIN_PENDING);
        assertThat(response.getCheckInStableSecondsRemaining()).isNotNull();
        assertThat(response.getActionTaken()).isNull();
    }

    @Test
    void requiresWfhConfirmationWhenOutsideAndNotCheckedIn() {
        LocalDate today = LocalDate.now();
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(true);
        when(cacheAdapter.get(anyString())).thenReturn(Optional.empty());
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder()
                        .officeId(1L)
                        .officeName("Pune Tech Park")
                        .distanceMeters(5000.0)
                        .withinFence(false)
                        .build());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());

        LocationSignalResponse response = locationSignalService.processLocationSignal(user, LocationPayload.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(10.0)
                .timestamp(LocalDateTime.now())
                .build());

        assertThat(response.isRequiresWfhConfirmation()).isTrue();
        assertThat(response.getTrackingState()).isEqualTo(AutoTrackingStateLabel.WFH_CONFIRMATION_REQUIRED);
    }

    @Test
    void reportsPoorAccuracyWhenInsideOfficeButGpsUnreliable() {
        LocalDate today = LocalDate.now();
        when(locationReliabilityService.isReliable(any(LocationPayload.class), eq(today))).thenReturn(false);
        when(cacheAdapter.get(anyString())).thenReturn(Optional.empty());
        when(employeeOfficeCacheService.getAssignedOffice(1L)).thenReturn(assignedOffice);
        when(assignedOfficeGeofenceService.evaluate(any(), anyDouble(), anyDouble())).thenReturn(
                GeoFenceMatchResult.builder()
                        .officeId(1L)
                        .officeName("Pune Tech Park")
                        .distanceMeters(50.0)
                        .withinFence(true)
                        .build());
        when(attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());

        LocationSignalResponse response = locationSignalService.processLocationSignal(user, LocationPayload.builder()
                .latitude(18.5912)
                .longitude(73.7389)
                .accuracy(250.0)
                .timestamp(LocalDateTime.now())
                .build());

        assertThat(response.isLocationReliable()).isFalse();
        assertThat(response.getTrackingState()).isEqualTo(AutoTrackingStateLabel.POOR_LOCATION_ACCURACY);
        verify(attendanceWriteService, org.mockito.Mockito.never()).recordTrackedEvent(
                any(), any(), any(), any(), any(), any(), any());
    }
}
