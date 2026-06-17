package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.geofence.dto.GeoFenceMatchResult;
import org.slf4j.Logger;

public final class GeofenceLoggingSupport {

    private GeofenceLoggingSupport() {
    }

    public static void logEvaluation(
            Logger log,
            Long employeeId,
            Long assignedOfficeId,
            GeoFenceMatchResult result,
            Double accuracyMeters,
            String source) {
        if (result == null) {
            return;
        }
        log.info(
                "Geofence evaluated employeeId={} officeId={} inside={} distanceMeters={} accuracy={} source={}",
                employeeId,
                assignedOfficeId != null ? assignedOfficeId : result.getOfficeId(),
                result.isWithinFence(),
                result.getDistanceMeters() != null ? Math.round(result.getDistanceMeters()) : null,
                accuracyMeters != null ? Math.round(accuracyMeters) : null,
                source);
    }
}
