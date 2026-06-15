package com.wfhwfo.attendance.employee.service;

import com.wfhwfo.attendance.audit.service.AuditService;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.employee.dto.CreateEmployeeRequest;
import com.wfhwfo.attendance.employee.dto.UpdateEmployeeRequest;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeAdminProjection;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.office.entity.OfficeLocation;
import com.wfhwfo.attendance.office.repository.OfficeLocationRepository;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import com.wfhwfo.attendance.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAdminServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private OfficeLocationRepository officeLocationRepository;
    @Mock
    private EmployeeOfficeCacheService employeeOfficeCacheService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private EmployeeAdminService employeeAdminService;

    @BeforeEach
    void setUp() {
        UserPrincipal admin = UserPrincipal.builder()
                .employeeId(8L)
                .email("admin@demo.com")
                .role(Role.ADMIN)
                .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        admin, null, admin.getAuthorities()));
    }

    @Test
    void createEmployeeSuccess() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("New Employee")
                .email("new.employee@demo.com")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .managerId(6L)
                .assignedOfficeLocationId(1L)
                .temporaryPassword("TempPass123")
                .active(true)
                .build();

        when(employeeRepository.existsByEmail("new.employee@demo.com")).thenReturn(false);
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(officeLocationRepository.findById(1L))
                .thenReturn(Optional.of(OfficeLocation.builder().id(1L).active(true).build()));
        when(employeeRepository.findByIdAndRoleAndActiveTrue(6L, Role.MANAGER))
                .thenReturn(Optional.of(Employee.builder().id(6L).role(Role.MANAGER).active(true).build()));
        when(passwordEncoder.encode("TempPass123")).thenReturn("hashed");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });
        when(employeeRepository.findEmployeeAdminById(99L)).thenReturn(Optional.of(projection(
                99L, "New Employee", "new.employee@demo.com", "EMPLOYEE", 1L, "Engineering", 6L, "Priya Manager", true)));

        var response = employeeAdminService.createEmployee(request);

        assertThat(response.getEmail()).isEqualTo("new.employee@demo.com");
        assertThat(response.getTeamName()).isEqualTo("Engineering");
        verify(auditService).log(8L, "EMPLOYEE_CREATED", "Employee", 99L, "Created employee new.employee@demo.com");
    }

    @Test
    void createEmployeeFailsWhenEmailExists() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("Duplicate")
                .email("employee@demo.com")
                .role(Role.EMPLOYEE)
                .teamId(1L)
                .temporaryPassword("TempPass123")
                .build();

        when(employeeRepository.existsByEmail("employee@demo.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeAdminService.createEmployee(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    void createEmployeeRequiresTeamForEmployeeRole() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("No Team")
                .email("noteam@demo.com")
                .role(Role.EMPLOYEE)
                .temporaryPassword("TempPass123")
                .build();

        when(employeeRepository.existsByEmail("noteam@demo.com")).thenReturn(false);

        assertThatThrownBy(() -> employeeAdminService.createEmployee(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Team is required");
    }

    private EmployeeAdminProjection projection(
            Long id, String name, String email, String role,
            Long teamId, String teamName, Long managerId, String managerName, boolean active) {
        return new EmployeeAdminProjection() {
            @Override
            public Long getId() { return id; }
            @Override
            public String getName() { return name; }
            @Override
            public String getEmail() { return email; }
            @Override
            public String getRole() { return role; }
            @Override
            public Long getTeamId() { return teamId; }
            @Override
            public String getTeamName() { return teamName; }
            @Override
            public Long getManagerId() { return managerId; }
            @Override
            public String getManagerName() { return managerName; }
            @Override
            public Long getAssignedOfficeLocationId() { return 1L; }
            @Override
            public String getAssignedOfficeName() { return "Pune Tech Park"; }
            @Override
            public String getAssignedOfficeAddress() { return "Pune, India"; }
            @Override
            public Boolean getActive() { return active; }
        };
    }
}
