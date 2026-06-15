package com.wfhwfo.attendance.employee.repository;

public interface EmployeeAdminProjection {

    Long getId();

    String getName();

    String getEmail();

    String getRole();

    Long getTeamId();

    String getTeamName();

    Long getManagerId();

    String getManagerName();

    Long getAssignedOfficeLocationId();

    String getAssignedOfficeName();

    String getAssignedOfficeAddress();

    Boolean getActive();
}
