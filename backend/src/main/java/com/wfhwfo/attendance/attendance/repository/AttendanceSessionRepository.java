package com.wfhwfo.attendance.attendance.repository;

import com.wfhwfo.attendance.attendance.entity.AttendanceSession;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByEmployeeIdAndAttendanceDateAndStatus(
            Long employeeId, LocalDate attendanceDate, AttendanceSessionStatus status);

    List<AttendanceSession> findByEmployeeIdAndAttendanceDateOrderByCheckInTimeAscIdAsc(
            Long employeeId, LocalDate attendanceDate);
}
