package com.wfhwfo.attendance.geofence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoFenceMatchResult {

    private Long officeId;
    private String officeName;
    private Double distanceMeters;
    private boolean withinFence;
}
