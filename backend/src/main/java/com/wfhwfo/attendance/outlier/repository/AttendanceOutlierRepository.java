package com.wfhwfo.attendance.outlier.repository;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceOutlierRepository extends JpaRepository<AttendanceOutlier, Long> {

    Page<AttendanceOutlier> findByTeamIdAndStatus(Long teamId, OutlierStatus status, Pageable pageable);

    long countByTeamIdAndStatus(Long teamId, OutlierStatus status);

    boolean existsByEmployeeIdAndOutlierTypeAndStatus(
            Long employeeId, OutlierType outlierType, OutlierStatus status);

    @Query("""
            SELECT o FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE e.managerId = :managerId AND o.status = :status
            ORDER BY o.detectedAt DESC
            """)
    Page<AttendanceOutlier> findByManagerIdAndStatus(
            @Param("managerId") Long managerId,
            @Param("status") OutlierStatus status,
            Pageable pageable);

    List<AttendanceOutlier> findByEmployeeIdAndStatus(Long employeeId, OutlierStatus status);

    @Query("""
            SELECT COUNT(o) FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE e.managerId = :managerId AND o.status = :status
            """)
    long countByManagerIdAndStatus(
            @Param("managerId") Long managerId,
            @Param("status") OutlierStatus status);

    @Query(value = """
            SELECT o FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE (:status IS NULL OR o.status = :status)
              AND (:severity IS NULL OR o.severity = :severity)
              AND (:type IS NULL OR o.outlierType = :type)
              AND (:managerId IS NULL OR e.managerId = :managerId)
            ORDER BY o.detectedAt DESC
            """,
            countQuery = """
            SELECT COUNT(o) FROM AttendanceOutlier o
            JOIN Employee e ON e.id = o.employeeId
            WHERE (:status IS NULL OR o.status = :status)
              AND (:severity IS NULL OR o.severity = :severity)
              AND (:type IS NULL OR o.outlierType = :type)
              AND (:managerId IS NULL OR e.managerId = :managerId)
            """)
    Page<AttendanceOutlier> searchOutliers(
            @Param("status") OutlierStatus status,
            @Param("severity") com.wfhwfo.attendance.common.enums.Severity severity,
            @Param("type") OutlierType type,
            @Param("managerId") Long managerId,
            Pageable pageable);
}
