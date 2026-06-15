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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimitService loginRateLimitService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        loginRateLimitService.checkAllowed(request.getEmail());

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .filter(Employee::isActive)
                .orElseThrow(() -> {
                    loginRateLimitService.recordFailedAttempt(request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            loginRateLimitService.recordFailedAttempt(request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        loginRateLimitService.resetAttempts(request.getEmail());

        UserPrincipal principal = toPrincipal(employee);
        String token = jwtService.generateToken(principal);
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
