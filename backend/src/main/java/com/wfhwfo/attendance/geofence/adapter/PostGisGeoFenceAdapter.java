package com.wfhwfo.attendance.geofence.adapter;

import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import com.wfhwfo.attendance.office.repository.OfficeLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostGisGeoFenceAdapter implements GeoFenceAdapter {

    private final OfficeLocationRepository officeLocationRepository;

    @Override
    public Optional<GeoFenceMatchResult> findMatchingOffice(double latitude, double longitude) {
        return officeLocationRepository.findNearestOfficeWithinGeofence(latitude, longitude)
                .map(match -> GeoFenceMatchResult.builder()
                        .officeId(match.getOfficeId())
                        .officeName(match.getOfficeName())
                        .distanceMeters(match.getDistanceMeters())
                        .withinFence(true)
                        .build());
    }
}
