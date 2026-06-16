package com.wfhwfo.attendance.policy.service;

import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.dto.PagedResponseMapper;
import com.wfhwfo.attendance.common.exception.BusinessException;
import com.wfhwfo.attendance.policy.dto.AttendancePolicyRequest;
import com.wfhwfo.attendance.policy.dto.AttendancePolicyResponse;
import com.wfhwfo.attendance.policy.entity.AttendancePolicy;
import com.wfhwfo.attendance.policy.repository.AttendancePolicyRepository;
import com.wfhwfo.attendance.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendancePolicyService {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<AttendancePolicyResponse> getAll() {
        return attendancePolicyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendancePolicyResponse> list(Pageable pageable) {
        Page<AttendancePolicy> page = attendancePolicyRepository.findAllByOrderByTeamIdAsc(pageable);
        return PagedResponseMapper.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendancePolicyResponse getById(Long id) {
        return attendancePolicyRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException("Attendance policy not found", "POLICY_NOT_FOUND"));
    }

    @Transactional
    public AttendancePolicyResponse create(AttendancePolicyRequest request) {
        validateTeam(request.getTeamId());
        AttendancePolicy policy = toEntity(new AttendancePolicy(), request);
        return toResponse(attendancePolicyRepository.save(policy));
    }

    @Transactional
    public AttendancePolicyResponse update(Long id, AttendancePolicyRequest request) {
        validateTeam(request.getTeamId());
        AttendancePolicy policy = attendancePolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Attendance policy not found", "POLICY_NOT_FOUND"));
        return toResponse(attendancePolicyRepository.save(toEntity(policy, request)));
    }

    @Transactional
    public void delete(Long id) {
        if (!attendancePolicyRepository.existsById(id)) {
            throw new BusinessException("Attendance policy not found", "POLICY_NOT_FOUND");
        }
        attendancePolicyRepository.deleteById(id);
    }

    private void validateTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new BusinessException("Team not found", "TEAM_NOT_FOUND");
        }
    }

    private AttendancePolicy toEntity(AttendancePolicy policy, AttendancePolicyRequest request) {
        policy.setTeamId(request.getTeamId());
        policy.setMinimumWfoDaysPerWeek(request.getMinimumWfoDaysPerWeek());
        policy.setStandardCheckInTime(request.getStandardCheckInTime());
        policy.setStandardCheckOutTime(request.getStandardCheckOutTime());
        policy.setLateThresholdMinutes(request.getLateThresholdMinutes());
        policy.setRequiredWfoMinutes(request.getRequiredWfoMinutes() != null
                ? request.getRequiredWfoMinutes() : 180);
        policy.setActive(request.isActive());
        return policy;
    }

    private AttendancePolicyResponse toResponse(AttendancePolicy policy) {
        return AttendancePolicyResponse.builder()
                .id(policy.getId())
                .teamId(policy.getTeamId())
                .minimumWfoDaysPerWeek(policy.getMinimumWfoDaysPerWeek())
                .standardCheckInTime(policy.getStandardCheckInTime())
                .standardCheckOutTime(policy.getStandardCheckOutTime())
                .lateThresholdMinutes(policy.getLateThresholdMinutes())
                .requiredWfoMinutes(policy.getRequiredWfoMinutes())
                .active(policy.isActive())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
