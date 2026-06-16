package com.wfhwfo.attendance.attendance.repository;

import com.wfhwfo.attendance.attendance.entity.AttendanceEvent;
import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, Long> {

    List<AttendanceEvent> findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(
            Long employeeId, LocalDate attendanceDate);

    Page<AttendanceEvent> findByEmployeeIdAndAttendanceDateOrderByEventTimeAscIdAsc(
            Long employeeId, LocalDate attendanceDate, Pageable pageable);

    Page<AttendanceEvent> findByEmployeeIdAndAttendanceDateBetweenOrderByEventTimeDescIdDesc(
            Long employeeId, LocalDate from, LocalDate to, Pageable pageable);

    boolean existsByEmployeeIdAndAttendanceDateAndEventType(
            Long employeeId, LocalDate attendanceDate, AttendanceEventType eventType);

    long countByEmployeeIdAndEventTypeAndAttendanceDateBetween(
            Long employeeId,
            AttendanceEventType eventType,
            LocalDate from,
            LocalDate to);
}
