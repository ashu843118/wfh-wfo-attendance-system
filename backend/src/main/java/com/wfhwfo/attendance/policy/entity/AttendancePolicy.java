package com.wfhwfo.attendance.policy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "minimum_wfo_days_per_week", nullable = false)
    private Integer minimumWfoDaysPerWeek;

    @Column(name = "standard_check_in_time", nullable = false)
    private LocalTime standardCheckInTime;

    @Column(name = "standard_check_out_time", nullable = false)
    private LocalTime standardCheckOutTime;

    @Column(name = "late_threshold_minutes", nullable = false)
    private Integer lateThresholdMinutes;

    @Column(name = "required_wfo_minutes", nullable = false)
    @Builder.Default
    private Integer requiredWfoMinutes = 180;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
