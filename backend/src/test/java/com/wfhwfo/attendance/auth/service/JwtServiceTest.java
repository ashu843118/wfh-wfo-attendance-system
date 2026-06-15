package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-for-unit-and-integration-tests-min-256-bits-long!!",
                3600000L);
    }

    @Test
    void generateAndValidateToken() {
        UserPrincipal principal = UserPrincipal.builder()
                .employeeId(1L)
                .email("employee@demo.com")
                .name("Ashutosh Kumar")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .managerId(6L)
                .build();

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();

        UserPrincipal parsed = jwtService.parseToken(token);
        assertThat(parsed.getEmail()).isEqualTo("employee@demo.com");
        assertThat(parsed.getEmployeeId()).isEqualTo(1L);
        assertThat(parsed.getRole()).isEqualTo(Role.EMPLOYEE);
    }
}
