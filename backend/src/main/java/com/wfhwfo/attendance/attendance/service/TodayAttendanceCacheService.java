package com.wfhwfo.attendance.attendance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.dto.TodayAttendanceStatusCache;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodayAttendanceCacheService {

    private static final String KEY_PREFIX = "attendance:today:";

    private final CacheAdapter cacheAdapter;
    private final AttendanceSessionService attendanceSessionService;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.today-attendance-ttl-seconds:180}")
    private long todayAttendanceTtlSeconds;

    public Optional<TodayAttendanceStatusCache> get(Long employeeId, LocalDate date) {
        String cacheKey = cacheKey(employeeId, date);
        return cacheAdapter.get(cacheKey).flatMap(json -> {
            try {
                log.debug("Today attendance cache hit employeeId={} date={} key={}", employeeId, date, cacheKey);
                return Optional.of(objectMapper.readValue(json, TodayAttendanceStatusCache.class));
            } catch (JsonProcessingException ex) {
                log.warn("Failed to parse today attendance cache employeeId={} date={}", employeeId, date);
                cacheAdapter.evict(cacheKey);
                return Optional.empty();
            }
        });
    }

    public TodayAttendanceStatusCache buildFromRecord(AttendanceRecord record) {
        AttendanceMode currentSessionMode = null;
        if (record.getCurrentSessionStatus() == CurrentSessionStatus.OPEN) {
            currentSessionMode = attendanceSessionService.findOpenSession(
                            record.getEmployeeId(), record.getAttendanceDate())
                    .map(AttendanceSession::getSessionMode)
                    .orElse(null);
        }

        boolean dayClosed = record.getStatus() == AttendanceStatus.MISSING_CHECKOUT
                || record.getStatus() == AttendanceStatus.SYSTEM_CLOSED;
        boolean hasOpenSession = record.getCurrentSessionStatus() == CurrentSessionStatus.OPEN;

        return TodayAttendanceStatusCache.builder()
                .id(record.getId())
                .attendanceDate(record.getAttendanceDate())
                .attendanceMode(record.getAttendanceMode())
                .currentSessionMode(currentSessionMode)
                .currentSessionStatus(record.getCurrentSessionStatus())
                .status(record.getStatus())
                .processingStatus(record.getProcessingStatus())
                .canCheckIn(!hasOpenSession && !dayClosed)
                .canCheckOut(hasOpenSession)
                .firstCheckInTime(record.getFirstCheckInTime())
                .finalCheckoutTime(record.getFinalCheckOutTime())
                .totalOfficeMinutes(record.getTotalOfficeMinutes())
                .matchedOfficeLocationId(record.getMatchedOfficeLocationId())
                .distanceFromOfficeMeters(record.getDistanceFromOfficeMeters())
                .build();
    }

    public void cache(AttendanceRecord record) {
        TodayAttendanceStatusCache status = buildFromRecord(record);
        String cacheKey = cacheKey(record.getEmployeeId(), record.getAttendanceDate());
        try {
            cacheAdapter.put(
                    cacheKey,
                    objectMapper.writeValueAsString(status),
                    Duration.ofSeconds(todayAttendanceTtlSeconds));
            log.debug(
                    "Today attendance cache put employeeId={} date={} key={} ttlSeconds={}",
                    record.getEmployeeId(),
                    record.getAttendanceDate(),
                    cacheKey,
                    todayAttendanceTtlSeconds);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to cache today attendance status", ex);
        }
    }

    public void evict(Long employeeId, LocalDate date) {
        String cacheKey = cacheKey(employeeId, date);
        cacheAdapter.evict(cacheKey);
        log.debug("Today attendance cache evicted employeeId={} date={} key={}", employeeId, date, cacheKey);
    }

    private String cacheKey(Long employeeId, LocalDate date) {
        return KEY_PREFIX + employeeId + ":" + date;
    }
}
