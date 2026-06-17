package com.wfhwfo.attendance.employee.repository;

import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.geofence.repository.GeofenceValidationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Employee> findByManagerIdAndActiveTrue(Long managerId);

    List<Employee> findByTeamIdAndActiveTrue(Long teamId);

    long countByTeamIdAndActiveTrue(Long teamId);

    long countByActiveTrue();

    long countByManagerIdAndActiveTrue(Long managerId);

    Optional<Employee> findByIdAndRoleAndActiveTrue(Long id, Role role);

    @Query(value = """
            SELECT e.id AS id,
                   e.name AS name,
                   e.email AS email,
                   e.role AS role,
                   e.team_id AS teamId,
                   t.name AS teamName,
                   e.manager_id AS managerId,
                   m.name AS managerName,
                   e.assigned_office_location_id AS assignedOfficeLocationId,
                   o.office_name AS assignedOfficeName,
                   o.address AS assignedOfficeAddress,
                   e.active AS active
            FROM employees e
            LEFT JOIN teams t ON t.id = e.team_id
            LEFT JOIN employees m ON m.id = e.manager_id
            LEFT JOIN office_locations o ON o.id = e.assigned_office_location_id
            WHERE e.id = :id
            """, nativeQuery = true)
    Optional<EmployeeAdminProjection> findEmployeeAdminById(@Param("id") Long id);

    @Query(value = """
            SELECT e.id AS id,
                   e.name AS name,
                   e.email AS email,
                   e.role AS role,
                   e.team_id AS teamId,
                   t.name AS teamName,
                   e.manager_id AS managerId,
                   m.name AS managerName,
                   e.assigned_office_location_id AS assignedOfficeLocationId,
                   o.office_name AS assignedOfficeName,
                   o.address AS assignedOfficeAddress,
                   e.active AS active
            FROM employees e
            LEFT JOIN teams t ON t.id = e.team_id
            LEFT JOIN employees m ON m.id = e.manager_id
            LEFT JOIN office_locations o ON o.id = e.assigned_office_location_id
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:role IS NULL OR :role = '' OR e.role = CAST(:role AS VARCHAR))
              AND (:teamId IS NULL OR e.team_id = :teamId)
              AND (:active IS NULL OR e.active = :active)
            ORDER BY e.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM employees e
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:role IS NULL OR :role = '' OR e.role = CAST(:role AS VARCHAR))
              AND (:teamId IS NULL OR e.team_id = :teamId)
              AND (:active IS NULL OR e.active = :active)
            """,
            nativeQuery = true)
    Page<EmployeeAdminProjection> searchEmployees(
            @Param("search") String search,
            @Param("role") String role,
            @Param("teamId") Long teamId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT new com.wfhwfo.attendance.employee.dto.ManagerOptionResponse(e.id, e.name, e.email)
            FROM Employee e
            WHERE e.role = com.wfhwfo.attendance.common.enums.Role.MANAGER
              AND e.active = true
            ORDER BY e.name ASC
            """)
    List<com.wfhwfo.attendance.employee.dto.ManagerOptionResponse> findActiveManagers();

    @Query(value = """
            SELECT o.id AS officeLocationId,
                   o.office_name AS officeName,
                   o.address AS address,
                   o.latitude AS latitude,
                   o.longitude AS longitude,
                   o.radius_meters AS radiusMeters,
                   o.active AS active
            FROM employees e
            JOIN office_locations o ON o.id = e.assigned_office_location_id
            WHERE e.id = :employeeId
            """, nativeQuery = true)
    Optional<com.wfhwfo.attendance.office.repository.EmployeeAssignedOfficeProjection> findAssignedOfficeByEmployeeId(
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT e.id FROM employees e
            WHERE e.assigned_office_location_id = :officeLocationId
            """, nativeQuery = true)
    List<Long> findEmployeeIdsByAssignedOfficeLocationId(@Param("officeLocationId") Long officeLocationId);

    @Query(value = """
            SELECT o.id AS officeId,
                   o.office_name AS officeName,
                   o.address AS officeAddress,
                   o.radius_meters AS radiusMeters,
                   ST_Distance(
                       o.geo_point,
                       ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                   ) AS distanceMeters,
                   ST_DWithin(
                       o.geo_point,
                       ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                       o.radius_meters
                   ) AS insideGeofence
            FROM employees e
            JOIN office_locations o ON o.id = e.assigned_office_location_id
            WHERE e.id = :employeeId
              AND e.active = true
              AND o.active = true
            """, nativeQuery = true)
    Optional<GeofenceValidationProjection> validateAssignedOfficeGeofence(
            @Param("employeeId") Long employeeId,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude);
}
