package com.wfhwfo.attendance.policy.repository;

import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, Long> {

    Page<AttendancePolicy> findAllByOrderByTeamIdAsc(Pageable pageable);

    Optional<AttendancePolicy> findByTeamIdAndActiveTrue(Long teamId);
}
