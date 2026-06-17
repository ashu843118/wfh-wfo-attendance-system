package com.wfhwfo.attendance.geofence.repository;

public interface GeofenceValidationProjection {

    Long getOfficeId();

    String getOfficeName();

    String getOfficeAddress();

    Double getRadiusMeters();

    Double getDistanceMeters();

    Boolean getInsideGeofence();
}
