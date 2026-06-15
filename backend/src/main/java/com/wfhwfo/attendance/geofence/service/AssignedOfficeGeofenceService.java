package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.office.dto.EmployeeAssignedOfficeDto;
import org.springframework.stereotype.Service;

@Service
public class AssignedOfficeGeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public GeoFenceMatchResult evaluate(EmployeeAssignedOfficeDto office, double latitude, double longitude) {
        if (office == null || !office.isActive()) {
            return GeoFenceMatchResult.builder()
                    .withinFence(false)
                    .distanceMeters(null)
                    .build();
        }

        double distanceMeters = haversineMeters(
                latitude, longitude, office.getLatitude(), office.getLongitude());
        boolean withinFence = distanceMeters <= office.getRadiusMeters();

        return GeoFenceMatchResult.builder()
                .officeId(office.getOfficeLocationId())
                .officeName(office.getOfficeName())
                .distanceMeters(distanceMeters)
                .withinFence(withinFence)
                .build();
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
