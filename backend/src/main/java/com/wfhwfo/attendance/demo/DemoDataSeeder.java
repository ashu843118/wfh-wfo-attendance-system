package com.wfhwfo.attendance.demo;

import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.attendance.repository.AttendanceEventRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.attendance.repository.AttendanceSessionRepository;
import com.wfhwfo.attendance.attendance.util.GeoPointUtils;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.notification.entity.Notification;
import com.wfhwfo.attendance.notification.repository.NotificationRepository;
import com.wfhwfo.attendance.office.entity.OfficeLocation;
import com.wfhwfo.attendance.office.repository.OfficeLocationRepository;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import com.wfhwfo.attendance.team.entity.Team;
import com.wfhwfo.attendance.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    private final DemoDataProperties properties;
    private final DemoCsvLoader csvLoader;
    private final DemoAttendanceGenerator attendanceGenerator;
    private final DemoOutlierNotificationGenerator outlierNotificationGenerator;
    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeLocationRepository officeLocationRepository;
    private final AttendancePolicyRepository attendancePolicyRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceOutlierRepository attendanceOutlierRepository;
    private final NotificationRepository notificationRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (employeeRepository.count() >= properties.getMinEmployeesToSkip()) {
            log.info("Demo data already present ({} employees). Skipping seed.", employeeRepository.count());
            return;
        }

        log.info("Seeding demo data from CSV and deterministic generator...");
        transactionTemplate.executeWithoutResult(status -> doSeed());
        log.info("Demo data seed completed: {} employees", employeeRepository.count());
    }

    protected void doSeed() {
        Map<String, Team> teamsByName = seedTeams();
        List<OfficeLocation> offices = seedOffices();
        seedPolicies(teamsByName);
        List<Employee> employees = seedEmployees(teamsByName);
        Map<Long, AttendancePolicy> policyByTeamId = attendancePolicyRepository.findAll().stream()
                .collect(Collectors.toMap(AttendancePolicy::getTeamId, policy -> policy));

        Map<Long, DemoOutlierProfile> outlierProfiles = assignOutlierProfiles(employees, teamsByName);

        DemoAttendanceGenerator.AttendanceGenerationResult attendanceResult = attendanceGenerator.generate(
                employees,
                policyByTeamId,
                offices,
                outlierProfiles,
                LocalDate.now(),
                properties.getAttendanceDays(),
                properties.getRandomSeed());

        List<AttendanceRecord> savedRecords = saveInBatches(attendanceResult.records(), attendanceRecordRepository::saveAll);

        java.util.Map<String, Long> summaryKeyToId = savedRecords.stream()
                .collect(Collectors.toMap(
                        record -> record.getEmployeeId() + ":" + record.getAttendanceDate(),
                        AttendanceRecord::getId));

        List<AttendanceSession> sessions = attendanceResult.sessions().stream()
                .peek(session -> session.setAttendanceRecordId(
                        summaryKeyToId.get(session.getEmployeeId() + ":" + session.getAttendanceDate())))
                .toList();
        List<AttendanceSession> savedSessions = saveInBatches(sessions, attendanceSessionRepository::saveAll);

        java.util.Map<String, Long> sessionKeyToId = savedSessions.stream()
                .collect(Collectors.toMap(
                        session -> session.getEmployeeId() + ":" + session.getAttendanceDate(),
                        AttendanceSession::getId));

        List<AttendanceEvent> events = attendanceResult.events().stream()
                .peek(event -> {
                    String dayKey = event.getEmployeeId() + ":" + event.getAttendanceDate();
                    event.setAttendanceRecordId(summaryKeyToId.get(dayKey));
                    event.setAttendanceSessionId(sessionKeyToId.get(dayKey));
                })
                .toList();
        saveInBatches(events, attendanceEventRepository::saveAll);

        DemoOutlierNotificationGenerator.OutlierNotificationResult outlierResult =
                outlierNotificationGenerator.generate(employees, outlierProfiles, savedRecords);

        saveInBatches(outlierResult.outliers(), attendanceOutlierRepository::saveAll);
        saveInBatches(outlierResult.notifications(), notificationRepository::saveAll);
    }

    private Map<String, Team> seedTeams() {
        Map<String, Team> teamsByName = new HashMap<>();
        List<Team> teams = csvLoader.loadTeams().stream()
                .map(row -> Team.builder().name(row.name()).description(row.description()).build())
                .toList();
        teamRepository.saveAll(teams).forEach(team -> teamsByName.put(team.getName(), team));
        return teamsByName;
    }

    private List<OfficeLocation> seedOffices() {
        List<OfficeLocation> offices = csvLoader.loadOffices().stream()
                .map(row -> OfficeLocation.builder()
                        .officeName(row.officeName())
                        .address(row.address())
                        .latitude(row.latitude())
                        .longitude(row.longitude())
                        .geoPoint(GeoPointUtils.createPoint(row.latitude(), row.longitude()))
                        .radiusMeters(row.radiusMeters())
                        .active(row.active())
                        .build())
                .toList();
        return officeLocationRepository.saveAll(offices);
    }

    private void seedPolicies(Map<String, Team> teamsByName) {
        List<AttendancePolicy> policies = csvLoader.loadPolicies().stream()
                .map(row -> {
                    Team team = teamsByName.get(row.teamName());
                    if (team == null) {
                        throw new IllegalStateException("Unknown team in policy CSV: " + row.teamName());
                    }
                    return AttendancePolicy.builder()
                            .teamId(team.getId())
                            .minimumWfoDaysPerWeek(row.minimumWfoDaysPerWeek())
                            .standardCheckInTime(LocalTime.parse(row.standardCheckInTime()))
                            .standardCheckOutTime(LocalTime.parse(row.standardCheckOutTime()))
                            .lateThresholdMinutes(row.lateThresholdMinutes())
                            .requiredWfoMinutes(180)
                            .active(row.active())
                            .build();
                })
                .toList();
        attendancePolicyRepository.saveAll(policies);
    }

    private List<Employee> seedEmployees(Map<String, Team> teamsByName) {
        List<DemoCsvLoader.EmployeeRow> rows = csvLoader.loadEmployees();
        List<Employee> employees = new ArrayList<>();
        List<OfficeLocation> offices = officeLocationRepository.findByActiveTrue();

        for (DemoCsvLoader.EmployeeRow row : rows) {
            Long teamId = row.teamName() != null ? teamsByName.get(row.teamName()).getId() : null;
            Long assignedOfficeId = null;
            if (teamId != null && !offices.isEmpty()) {
                assignedOfficeId = offices.get((int) ((teamId - 1) % offices.size())).getId();
            }
            employees.add(Employee.builder()
                    .name(row.name())
                    .email(row.email())
                    .passwordHash(DEMO_PASSWORD_HASH)
                    .role(Role.valueOf(row.role()))
                    .teamId(teamId)
                    .assignedOfficeLocationId(assignedOfficeId)
                    .active(true)
                    .build());
        }

        employeeRepository.saveAll(employees);

        Map<String, Long> emailToId = employees.stream()
                .collect(Collectors.toMap(Employee::getEmail, Employee::getId));

        for (int i = 0; i < rows.size(); i++) {
            DemoCsvLoader.EmployeeRow row = rows.get(i);
            if (row.managerEmail() != null) {
                employees.get(i).setManagerId(emailToId.get(row.managerEmail()));
            }
        }

        return employeeRepository.saveAll(employees);
    }

    private Map<Long, DemoOutlierProfile> assignOutlierProfiles(List<Employee> employees, Map<String, Team> teamsByName) {
        Map<Long, DemoOutlierProfile> profiles = new HashMap<>();
        DemoOutlierProfile[] patterns = {
                DemoOutlierProfile.FREQUENT_LATE,
                DemoOutlierProfile.LOW_WFO,
                DemoOutlierProfile.FREQUENT_ABSENCE,
                DemoOutlierProfile.MISSING_CHECKOUT
        };

        teamsByName.values().stream()
                .sorted(Comparator.comparing(Team::getName))
                .forEach(team -> {
                    List<Employee> teamMembers = employees.stream()
                            .filter(employee -> employee.getRole() == Role.EMPLOYEE)
                            .filter(employee -> team.getId().equals(employee.getTeamId()))
                            .sorted(Comparator.comparing(Employee::getEmail))
                            .toList();

                    for (int i = 0; i < patterns.length && i < teamMembers.size(); i++) {
                        profiles.put(teamMembers.get(i).getId(), patterns[i]);
                    }
                });

        return profiles;
    }

    private <T> List<T> saveInBatches(List<T> items, java.util.function.Function<List<T>, List<T>> saver) {
        int batchSize = properties.getBatchSize();
        List<T> saved = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            saved.addAll(saver.apply(items.subList(i, end)));
        }
        return saved;
    }
}
