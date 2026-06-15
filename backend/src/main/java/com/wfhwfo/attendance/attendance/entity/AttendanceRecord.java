package com.wfhwfo.attendance.attendance.entity;

import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.CurrentSessionStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "first_check_in_time")
    private LocalDateTime firstCheckInTime;

    @Column(name = "final_check_out_time")
    private LocalDateTime finalCheckOutTime;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "check_in_accuracy")
    private Double checkInAccuracy;

    @Column(name = "check_in_geo_point", columnDefinition = "geography(Point,4326)")
    private Point checkInGeoPoint;

    @Column(name = "check_out_latitude")
    private Double checkOutLatitude;

    @Column(name = "check_out_longitude")
    private Double checkOutLongitude;

    @Column(name = "check_out_accuracy")
    private Double checkOutAccuracy;

    @Column(name = "check_out_geo_point", columnDefinition = "geography(Point,4326)")
    private Point checkOutGeoPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_mode", length = 20)
    private AttendanceMode attendanceMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_session_status", nullable = false, length = 20)
    @Builder.Default
    private CurrentSessionStatus currentSessionStatus = CurrentSessionStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ProcessingStatus processingStatus;

    private Boolean late;

    @Column(name = "distance_from_office_meters")
    private Double distanceFromOfficeMeters;

    @Column(name = "matched_office_location_id")
    private Long matchedOfficeLocationId;

    @Column(name = "total_office_minutes")
    private Integer totalOfficeMinutes;

    @Column(length = 50)
    private String source;

    @Column(length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
