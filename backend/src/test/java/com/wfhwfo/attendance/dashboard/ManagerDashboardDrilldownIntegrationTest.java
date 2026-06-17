package com.wfhwfo.attendance.dashboard;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ManagerDashboardDrilldownIntegrationTest {

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
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void managerDrilldownReturnsTeamEmployeesEvenWithSortQueryParam() throws Exception {
        String token = loginAndGetToken("manager@demo.com", "password");
        assertThat(token).isNotBlank();

        HttpHeaders headers = authHeaders(token);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/manager/dashboard/drilldown?type=TEAM_SIZE&page=0&size=10&sort=employeeName,asc",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("data").get("totalElements").asLong()).isGreaterThan(0);
        assertThat(body.get("data").get("content").isArray()).isTrue();
        assertThat(body.get("data").get("content").size()).isGreaterThan(0);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password),
                String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(loginResponse.getBody()).get("data").get("token").asText();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
