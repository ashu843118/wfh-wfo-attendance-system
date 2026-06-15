package com.wfhwfo.attendance.office.repository;

public interface EmployeeAssignedOfficeProjection {

    Long getOfficeLocationId();

    String getOfficeName();

    String getAddress();

    Double getLatitude();

    Double getLongitude();

    Integer getRadiusMeters();

    Boolean getActive();
}
