package com.wfhwfo.attendance.geofence.adapter;

import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;

import java.util.Optional;

public interface GeoFenceAdapter {

    Optional<GeoFenceMatchResult> findMatchingOffice(double latitude, double longitude);
}
