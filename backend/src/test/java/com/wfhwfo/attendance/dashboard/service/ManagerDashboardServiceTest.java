package com.wfhwfo.attendance.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wfhwfo.attendance.attendance.repository.AttendanceRecordRepository;
import com.wfhwfo.attendance.common.adapter.CacheAdapter;
import com.wfhwfo.attendance.common.enums.AttendanceMode;
import com.wfhwfo.attendance.common.enums.AttendanceStatus;
import com.wfhwfo.attendance.common.enums.ProcessingStatus;
import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import com.wfhwfo.attendance.dashboard.dto.ManagerDashboardResponse;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private AttendanceOutlierRepository attendanceOutlierRepository;
    @Mock
    private CacheAdapter cacheAdapter;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ManagerDashboardService managerDashboardService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(managerDashboardService, "dashboardTtlSeconds", 60L);
        UserPrincipal manager = UserPrincipal.builder()
                .employeeId(6L)
                .email("manager@demo.com")
                .role(Role.MANAGER)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(manager, null, manager.getAuthorities()));

        when(cacheAdapter.get(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teamTableIncludesAbsentEmployeesWithoutAttendanceRecord() throws Exception {
        LocalDate today = LocalDate.now();

        when(employeeRepository.countByManagerIdAndActiveTrue(6L)).thenReturn(2L);
        when(attendanceRecordRepository.countByManagerAndDateAndStatusIn(eq(6L), eq(today), any())).thenReturn(1L);
        when(attendanceRecordRepository.countByManagerDateAndMode(eq(6L), eq(today), eq(AttendanceMode.WFO))).thenReturn(1L);
        when(attendanceRecordRepository.countByManagerDateAndMode(eq(6L), eq(today), eq(AttendanceMode.WFH))).thenReturn(0L);
        when(attendanceRecordRepository.countLateByManagerAndDate(6L, today)).thenReturn(0L);
        when(attendanceRecordRepository.countByManagerDateAndProcessingStatus(
                eq(6L), eq(today), eq(ProcessingStatus.CLASSIFICATION_PENDING))).thenReturn(0L);
        when(attendanceOutlierRepository.countByManagerIdAndStatus(anyLong(), any())).thenReturn(0L);
        when(attendanceRecordRepository.countModeSplitByManagerAndDate(6L, today)).thenReturn(List.of());
        when(attendanceRecordRepository.monthlySummaryByManager(eq(6L), any(), eq(today))).thenReturn(List.of());

        when(attendanceRecordRepository.findTeamDashboardRows(6L, today)).thenReturn(List.of(
                new Object[]{1L, "Present Employee", AttendanceStatus.CHECKED_IN, AttendanceMode.WFO, false,
                        LocalDateTime.now(), null, ProcessingStatus.COMPLETED},
                new Object[]{2L, "Absent Employee", null, null, null, null, null, null}
        ));
        doReturn("{}").when(objectMapper).writeValueAsString(any());

        ManagerDashboardResponse response = managerDashboardService.getDashboardSummary(today);

        assertThat(response.getTeamTable()).hasSize(2);
        assertThat(response.getTeamTable().get(0).getStatus()).isEqualTo("CHECKED_IN");
        assertThat(response.getTeamTable().get(1).getEmployeeName()).isEqualTo("Absent Employee");
        assertThat(response.getTeamTable().get(1).getStatus()).isEqualTo("ABSENT");
    }
}
