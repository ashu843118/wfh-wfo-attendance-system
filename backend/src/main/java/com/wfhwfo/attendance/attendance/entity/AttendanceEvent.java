package com.wfhwfo.attendance.attendance.entity;

import com.wfhwfo.attendance.common.enums.AttendanceEventType;
import com.wfhwfo.attendance.common.enums.AttendanceTriggerMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceEvent {

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

    @Column(name = "attendance_session_id")
    private Long attendanceSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AttendanceEventType eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    private Double latitude;

    private Double longitude;

    private Double accuracy;

    @Column(name = "geo_point", columnDefinition = "geography(Point,4326)")
    private Point geoPoint;

    @Column(name = "matched_office_location_id")
    private Long matchedOfficeLocationId;

    @Column(nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode", nullable = false, length = 20)
    private AttendanceTriggerMode triggerMode;

    @Column(nullable = false)
    private boolean valid;

    @Column(length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
