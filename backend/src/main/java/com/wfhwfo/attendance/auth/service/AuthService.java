package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.auth.dto.LoginRequest;
import com.wfhwfo.attendance.auth.dto.LoginResponse;
import com.wfhwfo.attendance.auth.dto.UserProfileResponse;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimitService loginRateLimitService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        log.info("Login attempt email={}", request.getEmail());

        loginRateLimitService.checkAllowed(request.getEmail(), clientIp);

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .filter(Employee::isActive)
                .orElseThrow(() -> {
                    loginRateLimitService.recordFailedAttempt(request.getEmail(), clientIp);
                    log.warn("Login failed email={} reason=INVALID_CREDENTIALS", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            loginRateLimitService.recordFailedAttempt(request.getEmail(), clientIp);
            log.warn("Login failed email={} reason=INVALID_CREDENTIALS", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        loginRateLimitService.resetAttempts(request.getEmail());

        UserPrincipal principal = toPrincipal(employee);
        String token = jwtService.generateToken(principal);
        log.info("Login success userId={} role={}", employee.getId(), employee.getRole());
        return toLoginResponse(token, employee);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser() {
        UserPrincipal principal = SecurityUtils.currentUser();
        Employee employee = employeeRepository.findById(principal.getEmployeeId())
                .filter(Employee::isActive)
                .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND"));
        return toUserProfile(employee);
    }

    private UserPrincipal toPrincipal(Employee employee) {
        return UserPrincipal.builder()
                .employeeId(employee.getId())
                .email(employee.getEmail())
                .name(employee.getName())
                .role(employee.getRole())
                .teamId(employee.getTeamId())
                .managerId(employee.getManagerId())
                .build();
    }

    private LoginResponse toLoginResponse(String token, Employee employee) {
        return LoginResponse.builder()
                .token(token)
                .employeeId(employee.getId())
                .email(employee.getEmail())
                .name(employee.getName())
                .role(employee.getRole())
                .teamId(employee.getTeamId())
                .managerId(employee.getManagerId())
                .build();
    }

    private UserProfileResponse toUserProfile(Employee employee) {
        return UserProfileResponse.builder()
                .employeeId(employee.getId())
                .email(employee.getEmail())
                .name(employee.getName())
                .role(employee.getRole())
                .teamId(employee.getTeamId())
                .managerId(employee.getManagerId())
                .build();
    }
}
