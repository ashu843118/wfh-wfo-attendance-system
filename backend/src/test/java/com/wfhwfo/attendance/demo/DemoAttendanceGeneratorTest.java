package com.wfhwfo.attendance.demo;

import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.office.entity.OfficeLocation;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAttendanceGeneratorTest {

    private final DemoAttendanceGenerator generator = new DemoAttendanceGenerator();

    @Test
    void generatesDeterministicAttendanceWithExpectedMix() {
        Employee employee = Employee.builder()
                .id(42L)
                .email("eng.rahul.sharma@demo.com")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .active(true)
                .build();

        AttendancePolicy policy = AttendancePolicy.builder()
                .teamId(1L)
                .minimumWfoDaysPerWeek(2)
                .standardCheckInTime(LocalTime.of(9, 30))
                .standardCheckOutTime(LocalTime.of(18, 30))
                .lateThresholdMinutes(15)
                .active(true)
                .build();

        OfficeLocation office = OfficeLocation.builder()
                .id(1L)
                .officeName("Pune Tech Park")
                .latitude(18.5912)
                .longitude(73.7389)
                .radiusMeters(800)
                .active(true)
                .build();

        LocalDate endDate = LocalDate.of(2026, 6, 13);
        DemoAttendanceGenerator.AttendanceGenerationResult firstRun = generator.generate(
                List.of(employee),
                Map.of(1L, policy),
                List.of(office),
                Map.of(),
                endDate,
                30,
                42L);
        DemoAttendanceGenerator.AttendanceGenerationResult secondRun = generator.generate(
                List.of(employee),
                Map.of(1L, policy),
                List.of(office),
                Map.of(),
                endDate,
                30,
                42L);

        assertThat(firstRun.records()).isNotEmpty();
        assertThat(firstRun.records()).hasSameSizeAs(secondRun.records());
        assertThat(firstRun.records().get(0).getAttendanceMode()).isEqualTo(secondRun.records().get(0).getAttendanceMode());

        long wfo = firstRun.records().stream().filter(record -> record.getAttendanceMode() == AttendanceMode.WFO).count();
        long wfh = firstRun.records().stream().filter(record -> record.getAttendanceMode() == AttendanceMode.WFH).count();
        long totalWorkingDays = 22;

        assertThat(wfo + wfh).isLessThanOrEqualTo(totalWorkingDays);
        assertThat(wfo).isGreaterThan(wfh);
    }

    @Test
    void skipsTodayForDemoEmployeeLoginAccount() {
        Employee demoEmployee = Employee.builder()
                .id(7L)
                .email("employee@demo.com")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .active(true)
                .build();

        AttendancePolicy policy = AttendancePolicy.builder()
                .teamId(1L)
                .minimumWfoDaysPerWeek(2)
                .standardCheckInTime(LocalTime.of(9, 30))
                .standardCheckOutTime(LocalTime.of(18, 30))
                .lateThresholdMinutes(15)
                .active(true)
                .build();

        OfficeLocation office = OfficeLocation.builder()
                .latitude(18.5912)
                .longitude(73.7389)
                .radiusMeters(800)
                .active(true)
                .build();

        LocalDate today = LocalDate.now();
        DemoAttendanceGenerator.AttendanceGenerationResult result = generator.generate(
                List.of(demoEmployee),
                Map.of(1L, policy),
                List.of(office),
                new HashMap<>(),
                today,
                5,
                42L);

        assertThat(result.records()).noneMatch(record -> record.getAttendanceDate().equals(today));
    }
}
