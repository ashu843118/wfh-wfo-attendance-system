package com.wfhwfo.attendance.office.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficeLocationResponse {

    private Long id;
    private String officeName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
