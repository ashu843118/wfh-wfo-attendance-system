package com.wfhwfo.attendance.employee.dto;

import com.wfhwfo.attendance.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee record for admin listing")
public class EmployeeAdminResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long teamId;
    private String teamName;
    private Long managerId;
    private String managerName;
    private Long assignedOfficeLocationId;
    private String assignedOfficeName;
    private String assignedOfficeAddress;
    private boolean active;
}
