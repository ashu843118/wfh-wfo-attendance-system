package com.wfhwfo.attendance.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Activate or deactivate an employee")
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}
