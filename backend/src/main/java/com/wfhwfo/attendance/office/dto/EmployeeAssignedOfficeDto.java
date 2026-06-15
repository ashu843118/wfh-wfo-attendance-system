package com.wfhwfo.attendance.office.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAssignedOfficeDto {

    private Long officeLocationId;
    private String officeName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;
    private boolean active;
}
