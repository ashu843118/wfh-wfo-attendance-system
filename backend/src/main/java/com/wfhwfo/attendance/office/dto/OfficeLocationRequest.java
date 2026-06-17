package com.wfhwfo.attendance.office.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Request to create or update an office location")
public class OfficeLocationRequest {

    @NotBlank(message = "Office name is required")
    private String officeName;

    private String address;

    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @NotNull(message = "Radius is required")
    @Min(value = 50, message = "Radius must be at least 50 meters")
    @Max(value = 300, message = "Radius must not exceed 300 meters")
    @Schema(example = "100", description = "Geofence radius in meters (50–300). Default for new offices is 100.")
    private Integer radiusMeters;

    private boolean active = true;
}
