package com.wfhwfo.attendance.demo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoCsvLoaderTest {

    private final DemoCsvLoader loader = new DemoCsvLoader();

    @Test
    void loadsExpectedDemoReferenceData() {
        assertThat(loader.loadTeams()).hasSize(5);
        assertThat(loader.loadOffices()).hasSize(5);
        assertThat(loader.loadPolicies()).hasSize(5);
        assertThat(loader.loadEmployees()).hasSize(100);
        assertThat(loader.loadEmployees()).extracting(DemoCsvLoader.EmployeeRow::email)
                .contains("employee@demo.com", "manager@demo.com", "leader@demo.com", "admin@demo.com");
    }
}
