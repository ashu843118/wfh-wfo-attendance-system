package com.wfhwfo.attendance.demo;

import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.notification.entity.Notification;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DemoOutlierNotificationGenerator {

    public OutlierNotificationResult generate(
            List<Employee> employees,
            Map<Long, DemoOutlierProfile> outlierProfiles,
            List<AttendanceRecord> attendanceRecords) {

        Map<Long, Employee> employeeById = employees.stream()
                .collect(Collectors.toMap(Employee::getId, employee -> employee));

        List<AttendanceOutlier> outliers = new ArrayList<>();
        List<Notification> notifications = new ArrayList<>();
        LocalDateTime detectedAt = LocalDateTime.now().minusDays(1);

        outlierProfiles.forEach((employeeId, profile) -> {
            if (profile == DemoOutlierProfile.NONE) {
                return;
            }
            Employee employee = employeeById.get(employeeId);
            if (employee == null || employee.getManagerId() == null) {
                return;
            }

            OutlierType type = mapProfile(profile);
            Severity severity = profile == DemoOutlierProfile.LOW_WFO ? Severity.CRITICAL : Severity.WARNING;
            Long recordId = attendanceRecords.stream()
                    .filter(record -> record.getEmployeeId().equals(employeeId))
                    .map(AttendanceRecord::getId)
                    .findFirst()
                    .orElse(null);

            AttendanceOutlier outlier = AttendanceOutlier.builder()
                    .employeeId(employeeId)
                    .teamId(employee.getTeamId())
                    .attendanceRecordId(recordId)
                    .outlierType(type)
                    .severity(severity)
                    .description(buildDescription(employee.getName(), type))
                    .status(OutlierStatus.OPEN)
                    .detectedAt(detectedAt)
                    .build();
            outliers.add(outlier);

            notifications.add(Notification.builder()
                    .recipientEmployeeId(employee.getManagerId())
                    .relatedEmployeeId(employeeId)
                    .title("Team Outlier Alert")
                    .message(employee.getName() + " - " + buildDescription(employee.getName(), type))
                    .type("OUTLIER")
                    .severity(severity)
                    .read(false)
                    .build());
        });

        employees.stream()
                .filter(employee -> employee.getRole() == Role.EMPLOYEE)
                .limit(5)
                .forEach(employee -> notifications.add(Notification.builder()
                        .recipientEmployeeId(employee.getId())
                        .relatedEmployeeId(employee.getId())
                        .title("Attendance Reminder")
                        .message("Remember to check out before end of day.")
                        .type("ATTENDANCE")
                        .severity(Severity.INFO)
                        .read(true)
                        .build()));

        return new OutlierNotificationResult(outliers, notifications);
    }

    private OutlierType mapProfile(DemoOutlierProfile profile) {
        return switch (profile) {
            case FREQUENT_LATE -> OutlierType.FREQUENT_LATE_CHECK_IN;
            case LOW_WFO -> OutlierType.LOW_WFO_ATTENDANCE;
            case FREQUENT_ABSENCE -> OutlierType.FREQUENT_ABSENCE;
            case MISSING_CHECKOUT -> OutlierType.MISSING_CHECKOUT;
            case NONE -> OutlierType.FREQUENT_LATE_CHECK_IN;
        };
    }

    private String buildDescription(String name, OutlierType type) {
        return switch (type) {
            case FREQUENT_LATE_CHECK_IN -> name + " has frequent late check-ins in the last 7 working days.";
            case LOW_WFO_ATTENDANCE -> name + " has WFO attendance below the team policy threshold.";
            case FREQUENT_ABSENCE -> name + " has frequent absences in the last 14 working days.";
            case MISSING_CHECKOUT -> name + " has multiple days with missing check-out.";
            case REPEATED_SYSTEM_DAY_CLOSE -> name + " has repeated system day-close events in the last 30 days.";
        };
    }

    public record OutlierNotificationResult(List<AttendanceOutlier> outliers, List<Notification> notifications) {
    }
}
