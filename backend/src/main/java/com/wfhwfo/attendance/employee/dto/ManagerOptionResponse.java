package com.wfhwfo.attendance.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manager option for admin dropdown")
public class ManagerOptionResponse {

    private Long id;
    private String name;
    private String email;
}
