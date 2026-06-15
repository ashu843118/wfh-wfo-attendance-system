package com.wfhwfo.attendance.attendance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class AttendanceCheckInIntegrationTest {

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
        registry.add("app.outbox.poll-interval-ms", () -> "1000");
        registry.add("app.demo.seed.enabled", () -> "true");
        registry.add("app.demo.seed.min-employees-to-skip", () -> "90");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void employeeCheckInIsClassifiedAsWfoNearOffice() {
        String token = loginAndGetToken("employee@demo.com", "password");
        assertThat(token).isNotBlank();

        HttpHeaders headers = authHeaders(token);
        Map<String, Object> checkInBody = Map.of(
                "location", Map.of(
                        "latitude", 18.5912,
                        "longitude", 73.7389,
                        "accuracy", 12.5,
                        "timestamp", java.time.LocalDateTime.now().toString()
                ),
                "source", "INTEGRATION_TEST"
        );

        ResponseEntity<String> checkInResponse = restTemplate.postForEntity(
                "/api/attendance/check-in",
                new HttpEntity<>(checkInBody, headers),
                String.class);

        assertThat(checkInResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkInResponse.getBody()).contains("CLASSIFICATION_PENDING");

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            ResponseEntity<String> todayResponse = restTemplate.exchange(
                    "/api/attendance/me/today",
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)),
                    String.class);

            assertThat(todayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode data = objectMapper.readTree(todayResponse.getBody()).path("data");
            assertThat(data.path("processingStatus").asText()).isEqualTo("COMPLETED");
            assertThat(data.path("attendanceMode").asText()).isEqualTo("WFO");
        });
    }

    private String loginAndGetToken(String email, String password) {
        Map<String, String> loginBody = Map.of("email", email, "password", password);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(loginBody, jsonHeaders()),
                String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return objectMapper.readTree(loginResponse.getBody()).path("data").path("token").asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse login response", ex);
        }
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
