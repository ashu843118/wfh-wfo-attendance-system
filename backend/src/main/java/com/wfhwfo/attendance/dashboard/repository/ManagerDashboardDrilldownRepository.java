package com.wfhwfo.attendance.dashboard.repository;

import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.OutlierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

public interface ManagerDashboardDrilldownRepository extends JpaRepository<AttendanceRecord, Long> {

    String EMPLOYEE_DRILLDOWN_SELECT = """
            SELECT e.id, e.name, e.email, ol.officeName,
                   ar.status, ar.attendanceMode, ar.firstCheckInTime, ar.finalCheckOutTime,
                   ar.totalOfficeMinutes, ar.currentSessionStatus,
                   (SELECT COUNT(o) FROM AttendanceOutlier o
                    WHERE o.employeeId = e.id AND o.status = com.wfhwfo.attendance.common.enums.OutlierStatus.OPEN)
            """;

    String EMPLOYEE_DRILLDOWN_FROM = """
            FROM Employee e
            LEFT JOIN OfficeLocation ol ON ol.id = e.assignedOfficeLocationId
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
            """;

    @Query(value = EMPLOYEE_DRILLDOWN_SELECT + EMPLOYEE_DRILLDOWN_FROM + " ORDER BY e.name",
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            WHERE e.managerId = :managerId AND e.active = true
            """)
    Page<Object[]> findTeamSizeDrilldown(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query(value = EMPLOYEE_DRILLDOWN_SELECT + """
            FROM Employee e
            LEFT JOIN OfficeLocation ol ON ol.id = e.assignedOfficeLocationId
            JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
              AND ar.status IN :presentStatuses
            ORDER BY e.name
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
              AND ar.status IN :presentStatuses
            """)
    Page<Object[]> findPresentDrilldown(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("presentStatuses") List<AttendanceStatus> presentStatuses,
            Pageable pageable);

    @Query(value = EMPLOYEE_DRILLDOWN_SELECT + """
            FROM Employee e
            LEFT JOIN OfficeLocation ol ON ol.id = e.assignedOfficeLocationId
            JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
              AND ar.attendanceMode = :mode
              AND ar.processingStatus = com.wfhwfo.attendance.common.enums.ProcessingStatus.COMPLETED
            ORDER BY e.name
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
              AND ar.attendanceMode = :mode
              AND ar.processingStatus = com.wfhwfo.attendance.common.enums.ProcessingStatus.COMPLETED
            """)
    Page<Object[]> findModeDrilldown(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("mode") AttendanceMode mode,
            Pageable pageable);

    @Query(value = EMPLOYEE_DRILLDOWN_SELECT + EMPLOYEE_DRILLDOWN_FROM + """
              AND (ar.id IS NULL OR ar.status NOT IN :presentStatuses)
            ORDER BY e.name
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
              AND (ar.id IS NULL OR ar.status NOT IN :presentStatuses)
            """)
    Page<Object[]> findAbsentDrilldown(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("presentStatuses") List<AttendanceStatus> presentStatuses,
            Pageable pageable);

    @Query(value = """
            SELECT o.id, e.id, e.name, o.outlierType, o.severity, o.description, o.detectedAt, o.status
            FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE e.managerId = :managerId AND o.status = :status
            ORDER BY o.detectedAt DESC
            """,
            countQuery = """
            SELECT COUNT(o) FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE e.managerId = :managerId AND o.status = :status
            """)
    Page<Object[]> findOutlierDrilldown(
            @Param("managerId") Long managerId,
            @Param("status") OutlierStatus status,
            Pageable pageable);
}
