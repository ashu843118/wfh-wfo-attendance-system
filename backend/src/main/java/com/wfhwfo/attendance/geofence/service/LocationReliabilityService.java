package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.attendance.config.AutoAttendanceProperties;
import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LocationReliabilityService {

    private final AutoAttendanceProperties autoAttendanceProperties;

    public boolean isReliable(LocationPayload location, LocalDate attendanceDate) {
        if (location.getAccuracy() == null
                || location.getAccuracy() > autoAttendanceProperties.getMaxAccuracyMeters()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reference = resolveReferenceTime(location, attendanceDate, now);
        long ageSeconds = Math.abs(Duration.between(reference, now).getSeconds());
        return ageSeconds <= autoAttendanceProperties.getMaxLocationAgeSeconds();
    }

    private LocalDateTime resolveReferenceTime(LocationPayload location, LocalDate attendanceDate, LocalDateTime now) {
        if (location.getTimestamp() == null) {
            return now;
        }
        LocalDateTime clientTime = location.getTimestamp();
        if (clientTime.isAfter(now.plusMinutes(2)) || clientTime.isBefore(attendanceDate.atStartOfDay())) {
            return now;
        }
        return clientTime;
    }
}
