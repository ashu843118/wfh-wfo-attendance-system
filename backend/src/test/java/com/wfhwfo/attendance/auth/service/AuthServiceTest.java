package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.auth.dto.LoginRequest;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginRateLimitService loginRateLimitService;

    @InjectMocks
    private AuthService authService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder()
                .id(1L)
                .name("Ashutosh Kumar")
                .email("employee@demo.com")
                .passwordHash("hash")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .managerId(6L)
                .active(true)
                .build();
    }

    @Test
    void loginSuccess() {
        when(employeeRepository.findByEmail("employee@demo.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("employee@demo.com", "password"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("employee@demo.com");
        assertThat(response.getRole()).isEqualTo(Role.EMPLOYEE);
    }

    @Test
    void loginFailureInvalidPassword() {
        when(employeeRepository.findByEmail("employee@demo.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("employee@demo.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFailureUnknownUser() {
        when(employeeRepository.findByEmail("unknown@demo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@demo.com", "password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFailureInactiveUser() {
        employee.setActive(false);
        when(employeeRepository.findByEmail("employee@demo.com")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.login(new LoginRequest("employee@demo.com", "password")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
