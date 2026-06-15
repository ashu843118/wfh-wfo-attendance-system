package com.wfhwfo.attendance.employee.dto;

import com.wfhwfo.attendance.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to create a new employee")
public class CreateEmployeeRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "John Doe")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "john.doe@demo.com")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    private Long teamId;

    private Long managerId;

    private Long assignedOfficeLocationId;

    @NotBlank(message = "Temporary password is required")
    @Size(min = 6, message = "Temporary password must be at least 6 characters")
    @Schema(example = "TempPass123")
    private String temporaryPassword;

    @Builder.Default
    private boolean active = true;
}
