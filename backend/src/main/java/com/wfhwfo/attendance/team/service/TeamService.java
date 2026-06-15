package com.wfhwfo.attendance.team.service;

import com.wfhwfo.attendance.team.dto.TeamOptionResponse;
import com.wfhwfo.attendance.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public List<TeamOptionResponse> getAllTeams() {
        return teamRepository.findAllByOrderByNameAsc().stream()
                .map(team -> TeamOptionResponse.builder()
                        .id(team.getId())
                        .name(team.getName())
                        .build())
                .toList();
    }
}
