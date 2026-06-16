package com.wfhwfo.attendance.attendance.repository;

import com.wfhwfo.attendance.attendance.entity.AttendanceRecord;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    Page<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDescFirstCheckInTimeDesc(
            Long employeeId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    @Query("""
            SELECT ar FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date
            ORDER BY e.name
            """)
    Page<AttendanceRecord> findTeamAttendanceByManagerAndDate(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query("""
            SELECT ar FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.employeeId = :employeeId
              AND ar.attendanceDate BETWEEN :from AND :to
            ORDER BY ar.attendanceDate DESC
            """)
    Page<AttendanceRecord> findEmployeeAttendanceForManager(
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date
              AND ar.status IN :statuses
            """)
    long countByManagerAndDateAndStatusIn(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("statuses") List<AttendanceStatus> statuses);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date
              AND ar.attendanceMode = :mode AND ar.processingStatus = 'COMPLETED'
            """)
    long countByManagerDateAndMode(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("mode") AttendanceMode mode);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date
              AND ar.processingStatus = :processingStatus
            """)
    long countByManagerDateAndProcessingStatus(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            @Param("processingStatus") ProcessingStatus processingStatus);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date AND ar.late = true
            """)
    long countLateByManagerAndDate(@Param("managerId") Long managerId, @Param("date") LocalDate date);

    @Query("""
            SELECT ar.attendanceMode, COUNT(ar) FROM AttendanceRecord ar
            JOIN Employee e ON e.id = ar.employeeId
            WHERE e.managerId = :managerId AND ar.attendanceDate = :date
              AND ar.processingStatus = 'COMPLETED'
            GROUP BY ar.attendanceMode
            """)
    List<Object[]> countModeSplitByManagerAndDate(@Param("managerId") Long managerId, @Param("date") LocalDate date);

    @Query("""
            SELECT e.name, COUNT(CASE WHEN ar.status IN ('CHECKED_IN','CHECKED_OUT') THEN 1 END),
                   COUNT(CASE WHEN ar.attendanceMode = 'WFO' AND ar.processingStatus = 'COMPLETED' THEN 1 END),
                   COUNT(CASE WHEN ar.attendanceMode = 'WFH' AND ar.processingStatus = 'COMPLETED' THEN 1 END)
            FROM Employee e
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id
              AND ar.attendanceDate BETWEEN :from AND :to
            WHERE e.managerId = :managerId AND e.active = true
            GROUP BY e.id, e.name
            ORDER BY e.name
            """)
    List<Object[]> monthlySummaryByManager(
            @Param("managerId") Long managerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to
              AND ar.status IN ('CHECKED_IN','CHECKED_OUT')
            """)
    long countPresentDays(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to
              AND ar.attendanceMode = :mode AND ar.processingStatus = 'COMPLETED'
            """)
    long countModeDays(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                       @Param("to") LocalDate to, @Param("mode") AttendanceMode mode);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to AND ar.late = true
            """)
    long countLateDays(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.processingStatus = :status
              AND ar.attendanceDate BETWEEN :from AND :to
            """)
    long countProcessingStatus(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                               @Param("to") LocalDate to, @Param("status") ProcessingStatus status);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.late = true
              AND ar.firstCheckInTime >= :since
            """)
    long countLateSince(@Param("employeeId") Long employeeId, @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to
              AND ar.attendanceMode = 'WFO' AND ar.processingStatus = 'COMPLETED'
            """)
    long countWfoDays(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT ar FROM AttendanceRecord ar
            WHERE ar.attendanceDate = :date AND ar.firstCheckInTime IS NOT NULL
              AND ar.status = 'CHECKED_IN'
            """)
    List<AttendanceRecord> findMissingCheckouts(@Param("date") LocalDate date);

    @Query("""
            SELECT ar FROM AttendanceRecord ar
            WHERE ar.attendanceDate = :date
              AND ar.firstCheckInTime IS NOT NULL
              AND ar.finalCheckOutTime IS NULL
              AND ar.status = com.wfhwfo.attendance.common.enums.AttendanceStatus.CHECKED_IN
            """)
    List<AttendanceRecord> findOpenRecordsForDayClose(@Param("date") LocalDate date);

    @Query("""
            SELECT ar.teamId,
                   COUNT(CASE WHEN ar.status IN ('CHECKED_IN','CHECKED_OUT') THEN 1 END) * 100.0 / NULLIF(COUNT(ar), 0)
            FROM AttendanceRecord ar
            WHERE ar.attendanceDate BETWEEN :from AND :to AND ar.teamId IS NOT NULL
            GROUP BY ar.teamId
            """)
    List<Object[]> teamAttendancePercentages(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT ar.attendanceDate,
                   COUNT(CASE WHEN ar.attendanceMode = 'WFO' AND ar.processingStatus = 'COMPLETED' THEN 1 END),
                   COUNT(CASE WHEN ar.attendanceMode = 'WFH' AND ar.processingStatus = 'COMPLETED' THEN 1 END)
            FROM AttendanceRecord ar
            WHERE ar.attendanceDate BETWEEN :from AND :to
            GROUP BY ar.attendanceDate
            ORDER BY ar.attendanceDate
            """)
    List<Object[]> wfoWfhTrend(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.attendanceDate = :date AND ar.status IN :statuses
            """)
    long countByDateAndStatusIn(@Param("date") LocalDate date, @Param("statuses") List<AttendanceStatus> statuses);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.attendanceDate = :date
              AND ar.attendanceMode = :mode AND ar.processingStatus = 'COMPLETED'
            """)
    long countByDateAndMode(@Param("date") LocalDate date, @Param("mode") AttendanceMode mode);

    @Query("""
            SELECT e.teamId, t.name,
                   SUM(CASE WHEN ar.status IN ('CHECKED_IN','CHECKED_OUT') THEN 1 ELSE 0 END) * 100.0
                       / NULLIF(COUNT(DISTINCT e.id) * :workingDays, 0)
            FROM Employee e
            JOIN Team t ON t.id = e.teamId
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id
              AND ar.attendanceDate BETWEEN :from AND :to
            WHERE e.active = true
            GROUP BY e.teamId, t.name
            ORDER BY t.name
            """)
    List<Object[]> teamAttendancePercentagesWithNames(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("workingDays") long workingDays);

    @Query("""
            SELECT e.id, e.name, ar.status, ar.attendanceMode, ar.late,
                   ar.firstCheckInTime, ar.finalCheckOutTime, ar.processingStatus
            FROM Employee e
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
            ORDER BY e.name
            """)
    List<Object[]> findTeamDashboardRows(@Param("managerId") Long managerId, @Param("date") LocalDate date);

    @Query(value = """
            SELECT e.id, e.name, ar.status, ar.attendanceMode, ar.late,
                   ar.firstCheckInTime, ar.finalCheckOutTime, ar.processingStatus
            FROM Employee e
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id AND ar.attendanceDate = :date
            WHERE e.managerId = :managerId AND e.active = true
            ORDER BY e.name
            """,
            countQuery = """
            SELECT COUNT(e) FROM Employee e
            WHERE e.managerId = :managerId AND e.active = true
            """)
    Page<Object[]> findTeamDashboardRowsPage(
            @Param("managerId") Long managerId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query(value = """
            SELECT e.teamId, t.name,
                   SUM(CASE WHEN ar.status IN ('CHECKED_IN','CHECKED_OUT') THEN 1 ELSE 0 END) * 100.0
                       / NULLIF(COUNT(DISTINCT e.id) * :workingDays, 0)
            FROM Employee e
            JOIN Team t ON t.id = e.teamId
            LEFT JOIN AttendanceRecord ar ON ar.employeeId = e.id
              AND ar.attendanceDate BETWEEN :from AND :to
            WHERE e.active = true
            GROUP BY e.teamId, t.name
            ORDER BY t.name
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.teamId) FROM Employee e
            WHERE e.active = true AND e.teamId IS NOT NULL
            """)
    Page<Object[]> teamAttendancePercentagesWithNamesPage(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("workingDays") long workingDays,
            Pageable pageable);

    @Query("""
            SELECT COUNT(ar) FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to
              AND ar.status = com.wfhwfo.attendance.common.enums.AttendanceStatus.MISSING_CHECKOUT
            """)
    long countMissingCheckoutDays(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT ar.attendanceDate, ar.attendanceMode, ar.processingStatus
            FROM AttendanceRecord ar
            WHERE ar.employeeId = :employeeId AND ar.attendanceDate BETWEEN :from AND :to
            ORDER BY ar.attendanceDate
            """)
    List<Object[]> employeeWfoWfhTrend(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
