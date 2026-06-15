package com.wfhwfo.attendance.auth.dto;

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
@Schema(description = "Authenticated user profile")
public class UserProfileResponse {

    private Long employeeId;
    private String email;
    private String name;
    private Role role;
    private Long teamId;
    private Long managerId;
}
