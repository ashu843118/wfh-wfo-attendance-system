package com.wfhwfo.attendance.employee.service;

import com.wfhwfo.attendance.audit.service.AuditService;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import com.wfhwfo.attendance.employee.dto.CreateEmployeeRequest;
import com.wfhwfo.attendance.employee.dto.EmployeeAdminResponse;
import com.wfhwfo.attendance.employee.dto.ManagerOptionResponse;
import com.wfhwfo.attendance.employee.dto.UpdateEmployeeRequest;
import com.wfhwfo.attendance.employee.dto.UpdateEmployeeStatusRequest;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeAdminProjection;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.office.service.EmployeeOfficeCacheService;
import com.wfhwfo.attendance.office.repository.OfficeLocationRepository;
import com.wfhwfo.attendance.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final OfficeLocationRepository officeLocationRepository;
    private final EmployeeOfficeCacheService employeeOfficeCacheService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeAdminResponse> listEmployees(
            String search,
            Role role,
            Long teamId,
            Boolean active,
            Pageable pageable) {

        String searchTerm = (search == null || search.isBlank()) ? "" : search.trim();
        String roleFilter = role == null ? "" : role.name();

        Page<EmployeeAdminProjection> page = employeeRepository.searchEmployees(
                searchTerm, roleFilter, teamId, active, pageable);

        List<EmployeeAdminResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<EmployeeAdminResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public EmployeeAdminResponse createEmployee(CreateEmployeeRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email is already in use", "EMPLOYEE_EMAIL_EXISTS");
        }

        validateRoleAssignments(request.getRole(), request.getTeamId(), request.getManagerId(), null);
        validateAssignedOffice(request.getRole(), request.getAssignedOfficeLocationId());

        Employee employee = Employee.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(request.getRole())
                .teamId(request.getTeamId())
                .managerId(request.getManagerId())
                .assignedOfficeLocationId(request.getAssignedOfficeLocationId())
                .active(request.isActive())
                .build();

        Employee saved = employeeRepository.save(employee);
        auditService.log(
                SecurityUtils.currentUser().getEmployeeId(),
                "EMPLOYEE_CREATED",
                "Employee",
                saved.getId(),
                "Created employee " + saved.getEmail());

        return getEmployeeAdminResponse(saved.getId());
    }

    @Transactional
    public EmployeeAdminResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found", "EMPLOYEE_NOT_FOUND"));

        validateRoleAssignments(request.getRole(), request.getTeamId(), request.getManagerId(), id);
        validateAssignedOffice(request.getRole(), request.getAssignedOfficeLocationId());

        employee.setName(request.getName().trim());
        employee.setRole(request.getRole());
        employee.setTeamId(request.getTeamId());
        employee.setManagerId(request.getManagerId());
        employee.setAssignedOfficeLocationId(request.getAssignedOfficeLocationId());
        employee.setActive(request.getActive());

        employeeRepository.save(employee);
        employeeOfficeCacheService.evictEmployeeOfficeCache(employee.getId());
        auditService.log(
                SecurityUtils.currentUser().getEmployeeId(),
                "EMPLOYEE_UPDATED",
                "Employee",
                employee.getId(),
                "Updated employee " + employee.getEmail());

        return getEmployeeAdminResponse(id);
    }

    @Transactional
    public EmployeeAdminResponse updateEmployeeStatus(Long id, UpdateEmployeeStatusRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found", "EMPLOYEE_NOT_FOUND"));

        employee.setActive(request.getActive());
        employeeRepository.save(employee);

        auditService.log(
                SecurityUtils.currentUser().getEmployeeId(),
                request.getActive() ? "EMPLOYEE_ACTIVATED" : "EMPLOYEE_DEACTIVATED",
                "Employee",
                employee.getId(),
                (request.getActive() ? "Activated " : "Deactivated ") + employee.getEmail());

        return getEmployeeAdminResponse(id);
    }

    @Transactional(readOnly = true)
    public List<ManagerOptionResponse> getActiveManagers() {
        return employeeRepository.findActiveManagers();
    }

    private EmployeeAdminResponse getEmployeeAdminResponse(Long id) {
        return employeeRepository.findEmployeeAdminById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Employee not found", "EMPLOYEE_NOT_FOUND"));
    }

    private void validateRoleAssignments(Role role, Long teamId, Long managerId, Long employeeId) {
        if (role == Role.EMPLOYEE || role == Role.MANAGER) {
            if (teamId == null) {
                throw new BusinessException("Team is required for " + role.name(), "EMPLOYEE_TEAM_REQUIRED");
            }
            if (!teamRepository.existsById(teamId)) {
                throw new BusinessException("Team not found", "TEAM_NOT_FOUND");
            }
        }

        if (managerId != null) {
            if (employeeId != null && managerId.equals(employeeId)) {
                throw new BusinessException("Employee cannot be their own manager", "EMPLOYEE_INVALID_MANAGER");
            }
            employeeRepository.findByIdAndRoleAndActiveTrue(managerId, Role.MANAGER)
                    .orElseThrow(() -> new BusinessException(
                            "Manager must be an active user with MANAGER role",
                            "EMPLOYEE_INVALID_MANAGER"));
        }
    }

    private EmployeeAdminResponse toResponse(EmployeeAdminProjection projection) {
        return EmployeeAdminResponse.builder()
                .id(projection.getId())
                .name(projection.getName())
                .email(projection.getEmail())
                .role(Role.valueOf(projection.getRole()))
                .teamId(projection.getTeamId())
                .teamName(projection.getTeamName())
                .managerId(projection.getManagerId())
                .managerName(projection.getManagerName())
                .assignedOfficeLocationId(projection.getAssignedOfficeLocationId())
                .assignedOfficeName(projection.getAssignedOfficeName())
                .assignedOfficeAddress(projection.getAssignedOfficeAddress())
                .active(Boolean.TRUE.equals(projection.getActive()))
                .build();
    }

    private void validateAssignedOffice(Role role, Long assignedOfficeLocationId) {
        if (role == Role.EMPLOYEE || role == Role.MANAGER) {
            if (assignedOfficeLocationId == null) {
                throw new BusinessException("Assigned office is required for " + role.name(), "EMPLOYEE_OFFICE_REQUIRED");
            }
            officeLocationRepository.findById(assignedOfficeLocationId)
                    .filter(office -> office.isActive())
                    .orElseThrow(() -> new BusinessException(
                            "Assigned office must be an active office location",
                            "EMPLOYEE_INVALID_OFFICE"));
        }
    }
}
