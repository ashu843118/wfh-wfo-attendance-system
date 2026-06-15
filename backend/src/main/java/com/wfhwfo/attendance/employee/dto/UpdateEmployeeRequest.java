package com.wfhwfo.attendance.employee.dto;

import com.wfhwfo.attendance.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to update an employee")
public class UpdateEmployeeRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Role is required")
    private Role role;

    private Long teamId;

    private Long managerId;

    private Long assignedOfficeLocationId;

    @NotNull(message = "Active status is required")
    private Boolean active;
}
