package com.wfhwfo.attendance.attendance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geo location payload captured from the client device")
public class LocationPayload {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    @Schema(example = "12.9716")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    @Schema(example = "77.5946")
    private Double longitude;

    @NotNull(message = "Accuracy is required")
    @DecimalMin(value = "0.0", message = "Accuracy must be non-negative")
    @Schema(example = "15.5", description = "GPS accuracy in meters")
    private Double accuracy;

    @NotNull(message = "Timestamp is required")
    @Schema(description = "Client-captured timestamp for the location reading")
    private LocalDateTime timestamp;
}
