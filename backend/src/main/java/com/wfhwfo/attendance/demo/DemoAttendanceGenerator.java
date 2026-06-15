package com.wfhwfo.attendance.demo;

import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.util.GeoPointUtils;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.office.entity.OfficeLocation;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DemoAttendanceGenerator {

    private static final double WFH_BASE_LAT = 18.5204;
    private static final double WFH_BASE_LNG = 73.8567;
    private static final String DEMO_EMPLOYEE_EMAIL = "employee@demo.com";

    public AttendanceGenerationResult generate(
            List<Employee> employees,
            Map<Long, AttendancePolicy> policyByTeamId,
            List<OfficeLocation> offices,
            Map<Long, DemoOutlierProfile> outlierProfiles,
            LocalDate endDate,
            int attendanceDays,
            long randomSeed) {

        Random random = new Random(randomSeed);
        LocalDate startDate = endDate.minusDays(attendanceDays - 1L);
        List<AttendanceRecord> records = new ArrayList<>();
        List<AttendanceEvent> events = new ArrayList<>();
        List<AttendanceSession> sessions = new ArrayList<>();
        List<PendingRecordRef> pendingRefs = new ArrayList<>();

        for (Employee employee : employees) {
            if (employee.getRole() != Role.EMPLOYEE || employee.getTeamId() == null) {
                continue;
            }

            AttendancePolicy policy = policyByTeamId.get(employee.getTeamId());
            DemoOutlierProfile profile = outlierProfiles.getOrDefault(employee.getId(), DemoOutlierProfile.NONE);
            OfficeLocation office = offices.get((int) ((employee.getTeamId() - 1) % offices.size()));

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (!isWorkingDay(date)) {
                    continue;
                }
                if (date.equals(endDate) && DEMO_EMPLOYEE_EMAIL.equals(employee.getEmail())) {
                    continue;
                }

                AttendanceDecision decision = decide(employee.getId(), date, profile, random, date.equals(endDate));
                if (decision.absent()) {
                    continue;
                }

                LocalTime checkInTime = policy.getStandardCheckInTime();
                if (decision.late()) {
                    checkInTime = checkInTime.plusMinutes(policy.getLateThresholdMinutes() + 5L + (decision.bucket() % 20));
                } else {
                    checkInTime = checkInTime.minusMinutes(5L + (decision.bucket() % 10));
                }

                LocalDateTime checkInDateTime = LocalDateTime.of(date, checkInTime);
                boolean missingCheckout = decision.missingCheckout();
                boolean pending = decision.pending();

                double lat;
                double lng;
                Double distanceMeters = null;
                if (decision.mode() == AttendanceMode.WFO) {
                    lat = office.getLatitude() + offset(decision.bucket(), 0.0004);
                    lng = office.getLongitude() + offset(decision.bucket(), 1, 0.0004);
                    distanceMeters = 40.0 + (decision.bucket() % 180);
                } else {
                    lat = WFH_BASE_LAT + offset(decision.bucket(), 0.02);
                    lng = WFH_BASE_LNG + offset(decision.bucket(), 1, 0.02);
                }

                AttendanceStatus status;
                LocalDateTime checkOutDateTime = null;
                Double checkOutLat = null;
                Double checkOutLng = null;

                if (missingCheckout) {
                    status = AttendanceStatus.CHECKED_IN;
                } else if (date.equals(endDate) && decision.bucket() % 5 == 0) {
                    status = AttendanceStatus.CHECKED_IN;
                } else {
                    status = AttendanceStatus.CHECKED_OUT;
                    checkOutDateTime = LocalDateTime.of(date, policy.getStandardCheckOutTime().plusMinutes(5));
                    checkOutLat = lat + 0.0001;
                    checkOutLng = lng + 0.0001;
                }

                AttendanceEventType checkInEventType = decision.mode() == AttendanceMode.WFH
                        ? AttendanceEventType.WFH_CONFIRMED_CHECK_IN
                        : AttendanceEventType.MANUAL_CHECK_IN;
                AttendanceEventType checkOutEventType = AttendanceEventType.MANUAL_CHECK_OUT;

                boolean openSession = missingCheckout || (date.equals(endDate) && decision.bucket() % 5 == 0);
                CurrentSessionStatus currentSessionStatus = openSession
                        ? CurrentSessionStatus.OPEN
                        : CurrentSessionStatus.CLOSED;

                int totalOfficeMinutes = 0;
                if (decision.mode() == AttendanceMode.WFO && checkOutDateTime != null) {
                    totalOfficeMinutes = (int) java.time.Duration.between(checkInDateTime, checkOutDateTime).toMinutes();
                }

                AttendanceRecord record = AttendanceRecord.builder()
                        .employeeId(employee.getId())
                        .teamId(employee.getTeamId())
                        .attendanceDate(date)
                        .firstCheckInTime(checkInDateTime)
                        .finalCheckOutTime(checkOutDateTime)
                        .checkInLatitude(lat)
                        .checkInLongitude(lng)
                        .checkInAccuracy(12.0 + (decision.bucket() % 8))
                        .checkInGeoPoint(GeoPointUtils.createPoint(lat, lng))
                        .checkOutLatitude(checkOutLat)
                        .checkOutLongitude(checkOutLng)
                        .checkOutAccuracy(checkOutLat != null ? 15.0 : null)
                        .checkOutGeoPoint(checkOutLat != null ? GeoPointUtils.createPoint(checkOutLat, checkOutLng) : null)
                        .attendanceMode(decision.mode())
                        .status(status)
                        .currentSessionStatus(currentSessionStatus)
                        .totalOfficeMinutes(totalOfficeMinutes > 0 ? totalOfficeMinutes : null)
                        .processingStatus(pending ? ProcessingStatus.CLASSIFICATION_PENDING : ProcessingStatus.COMPLETED)
                        .late(decision.late())
                        .distanceFromOfficeMeters(distanceMeters)
                        .matchedOfficeLocationId(decision.mode() == AttendanceMode.WFO ? office.getId() : null)
                        .source("DEMO_SEED")
                        .build();

                records.add(record);

                AttendanceSession session = AttendanceSession.builder()
                        .employeeId(employee.getId())
                        .teamId(employee.getTeamId())
                        .attendanceDate(date)
                        .sessionMode(decision.mode())
                        .checkInEventType(checkInEventType)
                        .checkInTime(checkInDateTime)
                        .checkInTriggerMode(AttendanceTriggerMode.MANUAL)
                        .checkOutTime(checkOutDateTime)
                        .checkOutEventType(checkOutDateTime != null ? checkOutEventType : null)
                        .autoCheckoutEligible(false)
                        .status(openSession ? AttendanceSessionStatus.OPEN : AttendanceSessionStatus.CLOSED)
                        .matchedOfficeLocationId(decision.mode() == AttendanceMode.WFO ? office.getId() : null)
                        .build();
                sessions.add(session);

                events.add(buildEvent(employee, date, checkInEventType, checkInDateTime, lat, lng,
                        12.0 + (decision.bucket() % 8)));
                if (checkOutDateTime != null) {
                    events.add(buildEvent(employee, date, checkOutEventType, checkOutDateTime,
                            checkOutLat, checkOutLng, 15.0));
                }
                if (pending) {
                    pendingRefs.add(new PendingRecordRef(employee.getId(), date));
                }
            }
        }

        return new AttendanceGenerationResult(records, events, sessions, pendingRefs);
    }

    private AttendanceEvent buildEvent(
            Employee employee,
            LocalDate date,
            AttendanceEventType eventType,
            LocalDateTime eventTime,
            Double lat,
            Double lng,
            double accuracy) {
        return AttendanceEvent.builder()
                .employeeId(employee.getId())
                .teamId(employee.getTeamId())
                .attendanceDate(date)
                .eventType(eventType)
                .eventTime(eventTime)
                .latitude(lat)
                .longitude(lng)
                .accuracy(accuracy)
                .geoPoint(lat != null && lng != null ? GeoPointUtils.createPoint(lat, lng) : null)
                .source("DEMO_SEED")
                .triggerMode(AttendanceTriggerMode.MANUAL)
                .valid(true)
                .build();
    }

    private AttendanceDecision decide(long employeeId, LocalDate date, DemoOutlierProfile profile, Random random, boolean isToday) {
        int bucket = Math.floorMod(employeeId * 31L + date.toEpochDay() * 17L, 100);

        if (profile == DemoOutlierProfile.FREQUENT_ABSENCE) {
            if (bucket < 18) {
                return AttendanceDecision.absent(bucket);
            }
        } else if (profile == DemoOutlierProfile.LOW_WFO) {
            if (bucket < 55) {
                return present(bucket, AttendanceMode.WFH, profile, random, isToday);
            }
        } else if (profile == DemoOutlierProfile.FREQUENT_LATE) {
            if (bucket < 8) {
                return AttendanceDecision.absent(bucket);
            }
            return present(bucket, AttendanceMode.WFO, DemoOutlierProfile.FREQUENT_LATE, random, isToday);
        } else if (profile == DemoOutlierProfile.MISSING_CHECKOUT) {
            if (bucket < 8) {
                return AttendanceDecision.absent(bucket);
            }
            AttendanceDecision decision = present(bucket, AttendanceMode.WFO, profile, random, isToday);
            if (bucket % 4 == 0) {
                return decision.withMissingCheckout(true);
            }
            return decision;
        }

        if (bucket < 7) {
            return AttendanceDecision.absent(bucket);
        }
        if (bucket < 37) {
            return present(bucket, AttendanceMode.WFH, profile, random, isToday);
        }
        return present(bucket, AttendanceMode.WFO, profile, random, isToday);
    }

    private AttendanceDecision present(int bucket, AttendanceMode mode, DemoOutlierProfile profile, Random random, boolean isToday) {
        boolean late = profile == DemoOutlierProfile.FREQUENT_LATE
                || (bucket >= 88 && bucket <= 98);
        boolean missingCheckout = bucket % 47 == 0;
        boolean pending = isToday && bucket % 23 == 0;
        return new AttendanceDecision(false, mode, late, missingCheckout, pending, bucket);
    }

    private boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private double offset(int bucket, double range) {
        return offset(bucket, 0, range);
    }

    private double offset(int bucket, int salt, double range) {
        int value = Math.floorMod(bucket * (salt + 3) + 11, 1000);
        return ((value / 1000.0) - 0.5) * range;
    }

    private record AttendanceDecision(
            boolean absent,
            AttendanceMode mode,
            boolean late,
            boolean missingCheckout,
            boolean pending,
            int bucket) {

        static AttendanceDecision absent(int bucket) {
            return new AttendanceDecision(true, null, false, false, false, bucket);
        }

        AttendanceDecision withMissingCheckout(boolean value) {
            return new AttendanceDecision(absent, mode, late, value, pending, bucket);
        }
    }

    public record PendingRecordRef(long employeeId, LocalDate date) {
    }

    public record AttendanceGenerationResult(
            List<AttendanceRecord> records,
            List<AttendanceEvent> events,
            List<AttendanceSession> sessions,
            List<PendingRecordRef> pendingRefs) {
    }
}
