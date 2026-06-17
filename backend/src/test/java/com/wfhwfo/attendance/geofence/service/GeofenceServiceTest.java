package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.attendance.config.AutoAttendanceProperties;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.geofence.repository.GeofenceValidationProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeofenceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private GeofenceValidationProjection projection;

    private GeofenceService geofenceService;

    @BeforeEach
    void setUp() {
        AutoAttendanceProperties properties = new AutoAttendanceProperties();
        properties.setMaxAccuracyMeters(100.0);
        geofenceService = new GeofenceService(employeeRepository, properties);
    }

    @Test
    void returnsInsideGeofenceFromPostGisProjection() {
        when(employeeRepository.validateAssignedOfficeGeofence(eq(1L), eq(12.9262), eq(77.6811)))
                .thenReturn(Optional.of(projection));
        when(projection.getOfficeId()).thenReturn(10L);
        when(projection.getOfficeName()).thenReturn("EY Bengaluru");
        when(projection.getOfficeAddress()).thenReturn("Ecospace");
        when(projection.getRadiusMeters()).thenReturn(100.0);
        when(projection.getDistanceMeters()).thenReturn(5.0);
        when(projection.getInsideGeofence()).thenReturn(true);

        var result = geofenceService.validateEmployeeLocation(
                1L,
                LocationPayload.builder().latitude(12.9262).longitude(77.6811).accuracy(10.0).build());

        assertThat(result.isInsideGeofence()).isTrue();
        assertThat(result.getDistanceMeters()).isEqualTo(5.0);
        assertThat(result.getOfficeId()).isEqualTo(10L);
    }

    @Test
    void returnsOutsideGeofenceFromPostGisProjection() {
        when(employeeRepository.validateAssignedOfficeGeofence(eq(1L), eq(13.0), eq(78.0)))
                .thenReturn(Optional.of(projection));
        when(projection.getOfficeId()).thenReturn(10L);
        when(projection.getOfficeName()).thenReturn("EY Bengaluru");
        when(projection.getOfficeAddress()).thenReturn("Ecospace");
        when(projection.getRadiusMeters()).thenReturn(100.0);
        when(projection.getDistanceMeters()).thenReturn(15000.0);
        when(projection.getInsideGeofence()).thenReturn(false);

        var result = geofenceService.validateEmployeeLocation(
                1L,
                LocationPayload.builder().latitude(13.0).longitude(78.0).accuracy(10.0).build());

        assertThat(result.isInsideGeofence()).isFalse();
        assertThat(result.getDistanceMeters()).isEqualTo(15000.0);
    }

    @Test
    void throwsWhenAssignedOfficeNotFound() {
        when(employeeRepository.validateAssignedOfficeGeofence(eq(99L), eq(12.9262), eq(77.6811)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> geofenceService.validateEmployeeLocation(
                99L,
                LocationPayload.builder().latitude(12.9262).longitude(77.6811).accuracy(10.0).build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ASSIGNED_OFFICE_NOT_FOUND");
    }

    @Test
    void rejectsPoorAccuracyForCheckInValidation() {
        assertThatThrownBy(() -> geofenceService.validateEmployeeLocationForCheckIn(
                1L,
                LocationPayload.builder().latitude(12.9262).longitude(77.6811).accuracy(250.0).build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOCATION_ACCURACY_POOR");
    }

    @Test
    void rejectsInvalidCoordinates() {
        assertThatThrownBy(() -> geofenceService.validateEmployeeLocation(
                1L,
                LocationPayload.builder().latitude(120.0).longitude(77.6811).accuracy(10.0).build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOCATION_INVALID");
    }
}
