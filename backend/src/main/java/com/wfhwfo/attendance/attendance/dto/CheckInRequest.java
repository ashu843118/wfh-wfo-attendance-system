package com.wfhwfo.attendance.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Check-in request with geo location")
public class CheckInRequest {

    @NotNull(message = "Location is required")
    @Valid
    private LocationPayload location;

    @Schema(example = "mobile-app")
    private String source;
}
