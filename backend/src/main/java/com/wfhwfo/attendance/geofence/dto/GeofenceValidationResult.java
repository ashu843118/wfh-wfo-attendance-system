package com.wfhwfo.attendance.geofence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceValidationResult {

    private Long officeId;
    private String officeName;
    private String officeAddress;
    private Double radiusMeters;
    private Double distanceMeters;
    private boolean insideGeofence;
    private Double accuracy;
}
