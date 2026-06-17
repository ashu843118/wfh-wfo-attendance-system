package com.wfhwfo.attendance.geofence.service;

import com.wfhwfo.attendance.attendance.dto.LocationPayload;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GeofenceServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("attendance_db")
            .withUsername("attendance_user")
            .withPassword("attendance_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.jwt.secret",
                () -> "test-secret-key-for-unit-and-integration-tests-min-256-bits-long!!");
        registry.add("app.demo.seed.enabled", () -> "true");
        registry.add("app.demo.seed.min-employees-to-skip", () -> "90");
    }

    @Autowired
    private GeofenceService geofenceService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void employeeInsideAssignedOfficeUsesPostGisDistanceAndInsideFlag() {
        Long employeeId = employeeRepository.findByEmail("employee@demo.com")
                .orElseThrow()
                .getId();

        var result = geofenceService.validateEmployeeLocation(
                employeeId,
                LocationPayload.builder()
                        .latitude(12.9262)
                        .longitude(77.6811)
                        .accuracy(10.0)
                        .build());

        assertThat(result.isInsideGeofence()).isTrue();
        assertThat(result.getDistanceMeters()).isNotNull();
        assertThat(result.getDistanceMeters()).isLessThan(100.0);
        assertThat(result.getOfficeName()).contains("EY Bengaluru");
    }

    @Test
    void employeeOutsideAssignedOfficeUsesPostGisDistanceAndInsideFlag() {
        Long employeeId = employeeRepository.findByEmail("employee@demo.com")
                .orElseThrow()
                .getId();

        var result = geofenceService.validateEmployeeLocation(
                employeeId,
                LocationPayload.builder()
                        .latitude(13.0)
                        .longitude(78.0)
                        .accuracy(10.0)
                        .build());

        assertThat(result.isInsideGeofence()).isFalse();
        assertThat(result.getDistanceMeters()).isGreaterThan(100.0);
    }

    @Test
    void missingAssignedOfficeFailsValidation() {
        assertThatThrownBy(() -> geofenceService.validateEmployeeLocation(
                999_999L,
                LocationPayload.builder()
                        .latitude(12.9262)
                        .longitude(77.6811)
                        .accuracy(10.0)
                        .build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ASSIGNED_OFFICE_NOT_FOUND");
    }
}
