package com.wfhwfo.attendance.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoCsvLoader {

    private static final String BASE_PATH = "demo-data/";

    public List<TeamRow> loadTeams() {
        return load(BASE_PATH + "teams.csv", cols -> new TeamRow(cols[0], cols[1]));
    }

    public List<OfficeRow> loadOffices() {
        return load(BASE_PATH + "office_locations.csv", cols -> new OfficeRow(
                cols[0], cols[1], Double.parseDouble(cols[2]), Double.parseDouble(cols[3]),
                Integer.parseInt(cols[4]), Boolean.parseBoolean(cols[5])));
    }

    public List<PolicyRow> loadPolicies() {
        return load(BASE_PATH + "attendance_policies.csv", cols -> new PolicyRow(
                cols[0], Integer.parseInt(cols[1]), cols[2], cols[3],
                Integer.parseInt(cols[4]), Boolean.parseBoolean(cols[5])));
    }

    public List<EmployeeRow> loadEmployees() {
        return load(BASE_PATH + "employees.csv", cols -> new EmployeeRow(
                cols[0], cols[1], cols[2], emptyToNull(cols[3]), emptyToNull(cols[4])));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> List<T> load(String path, RowMapper<T> mapper) {
        ClassPathResource resource = new ClassPathResource(path);
        List<T> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return rows;
            }
            header = stripBom(header);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(mapper.map(parseCsvLine(line)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load demo CSV: " + path, ex);
        }
        return rows;
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private String[] parseCsvLine(String line) {
        return line.split(",", -1);
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(String[] columns);
    }

    public record TeamRow(String name, String description) {
    }

    public record OfficeRow(String officeName, String address, double latitude, double longitude,
                            int radiusMeters, boolean active) {
    }

    public record PolicyRow(String teamName, int minimumWfoDaysPerWeek, String standardCheckInTime,
                            String standardCheckOutTime, int lateThresholdMinutes, boolean active) {
    }

    public record EmployeeRow(String name, String email, String role, String teamName, String managerEmail) {
    }
}
