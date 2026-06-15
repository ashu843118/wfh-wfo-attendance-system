package com.wfhwfo.attendance.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Team option for admin dropdown")
public class TeamOptionResponse {

    private Long id;
    private String name;
}
