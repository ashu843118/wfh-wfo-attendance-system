package com.wfhwfo.attendance.notification.service;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.OutlierType;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.notification.repository.NotificationRepository;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AttendanceOutlierRepository attendanceOutlierRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyForOutliersCreatesManagerNotification() {
        AttendanceOutlier outlier = AttendanceOutlier.builder()
                .employeeId(2L)
                .outlierType(OutlierType.FREQUENT_LATE_CHECK_IN)
                .severity(Severity.WARNING)
                .description("Late 4 times")
                .status(OutlierStatus.OPEN)
                .detectedAt(LocalDateTime.now())
                .build();

        when(attendanceOutlierRepository.findByEmployeeIdAndStatus(2L, OutlierStatus.OPEN))
                .thenReturn(List.of(outlier));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(
                Employee.builder().id(2L).name("Rahul Sharma").build()));
        when(notificationRepository.existsByRecipientEmployeeIdAndRelatedEmployeeIdAndTypeAndCreatedAtAfter(
                any(), any(), any(), any())).thenReturn(false);

        notificationService.notifyForOutliers(2L, 6L);

        verify(notificationRepository).save(any());
    }

    @Test
    void notifyForOutliersSkipsWhenManagerMissing() {
        notificationService.notifyForOutliers(2L, null);
        verify(notificationRepository, never()).save(any());
    }
}
