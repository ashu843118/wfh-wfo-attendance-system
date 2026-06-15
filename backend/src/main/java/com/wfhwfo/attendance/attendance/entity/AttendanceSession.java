package com.wfhwfo.attendance.attendance.entity;

import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceSessionStatus;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "attendance_record_id")
    private Long attendanceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 10)
    private AttendanceMode sessionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_event_type", nullable = false, length = 30)
    private AttendanceEventType checkInEventType;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_trigger_mode", nullable = false, length = 20)
    private AttendanceTriggerMode checkInTriggerMode;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_out_event_type", length = 30)
    private AttendanceEventType checkOutEventType;

    @Column(name = "auto_checkout_eligible", nullable = false)
    private boolean autoCheckoutEligible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSessionStatus status;

    @Column(name = "matched_office_location_id")
    private Long matchedOfficeLocationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
