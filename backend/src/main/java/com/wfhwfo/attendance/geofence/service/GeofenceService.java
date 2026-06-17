package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.attendance.config.AutoAttendanceProperties;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.geofence.dto.GeofenceValidationResult;
import com.wfhwfo.attendance.geofence.repository.GeofenceValidationProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeofenceService {

    private final EmployeeRepository employeeRepository;
    private final AutoAttendanceProperties autoAttendanceProperties;

    @Transactional(readOnly = true)
    public GeofenceValidationResult validateEmployeeLocation(
            Long employeeId,
            double latitude,
            double longitude,
            Double accuracy) {
        validateCoordinates(latitude, longitude);

        GeofenceValidationProjection projection = employeeRepository
                .validateAssignedOfficeGeofence(employeeId, latitude, longitude)
                .orElseThrow(() -> new BusinessException(
                        "No assigned active office configured for employee",
                        "ASSIGNED_OFFICE_NOT_FOUND"));

        return GeofenceValidationResult.builder()
                .officeId(projection.getOfficeId())
                .officeName(projection.getOfficeName())
                .officeAddress(projection.getOfficeAddress())
                .radiusMeters(projection.getRadiusMeters())
                .distanceMeters(projection.getDistanceMeters())
                .insideGeofence(Boolean.TRUE.equals(projection.getInsideGeofence()))
                .accuracy(accuracy)
                .build();
    }

    @Transactional(readOnly = true)
    public GeofenceValidationResult validateEmployeeLocation(Long employeeId, LocationPayload location) {
        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new BusinessException("Latitude and longitude are required", "LOCATION_REQUIRED");
        }
        return validateEmployeeLocation(
                employeeId,
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy());
    }

    @Transactional(readOnly = true)
    public GeofenceValidationResult validateEmployeeLocationForCheckIn(Long employeeId, LocationPayload location) {
        assertAcceptableAccuracy(location.getAccuracy());
        return validateEmployeeLocation(employeeId, location);
    }

    @Transactional(readOnly = true)
    public GeoFenceMatchResult validateForMatch(Long employeeId, LocationPayload location) {
        return toMatchResult(validateEmployeeLocation(employeeId, location));
    }

    @Transactional(readOnly = true)
    public GeoFenceMatchResult validateForCheckInMatch(Long employeeId, LocationPayload location) {
        return toMatchResult(validateEmployeeLocationForCheckIn(employeeId, location));
    }

    public GeoFenceMatchResult toMatchResult(GeofenceValidationResult validation) {
        return GeoFenceMatchResult.builder()
                .officeId(validation.getOfficeId())
                .officeName(validation.getOfficeName())
                .distanceMeters(validation.getDistanceMeters())
                .withinFence(validation.isInsideGeofence())
                .build();
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new BusinessException("Invalid latitude or longitude", "LOCATION_INVALID");
        }
    }

    private void assertAcceptableAccuracy(Double accuracy) {
        if (accuracy == null || accuracy > autoAttendanceProperties.getMaxAccuracyMeters()) {
            throw new BusinessException(
                    "Location accuracy is too poor for office check-in. Please try again with a better GPS signal.",
                    "LOCATION_ACCURACY_POOR");
        }
    }
}
