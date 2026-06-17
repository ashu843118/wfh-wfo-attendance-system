package com.wfhwfo.attendance.notification.service;

import com.wfhwfo.attendance.common.enums.OutlierStatus;
import com.wfhwfo.attendance.common.enums.Severity;
import com.wfhwfo.attendance.employee.entity.Employee;
import com.wfhwfo.attendance.employee.repository.EmployeeRepository;
import com.wfhwfo.attendance.notification.dto.NotificationResponse;
import com.wfhwfo.attendance.notification.entity.Notification;
import com.wfhwfo.attendance.notification.repository.NotificationRepository;
import com.wfhwfo.attendance.outlier.entity.AttendanceOutlier;
import com.wfhwfo.attendance.outlier.repository.AttendanceOutlierRepository;
import com.wfhwfo.attendance.common.dto.PagedResponse;
import com.wfhwfo.attendance.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AttendanceOutlierRepository attendanceOutlierRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public void notifyDayClose(Long employeeId, Long managerId, LocalDate attendanceDate) {
        String dateLabel = attendanceDate.toString();
        saveNotificationIfAbsent(
                employeeId,
                employeeId,
                "MISSING_CHECKOUT",
                "Attendance auto-closed",
                "Your attendance for " + dateLabel + " was auto-closed at day end because checkout was missing.",
                Severity.INFO);

        if (managerId != null) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            String employeeName = employee != null ? employee.getName() : "Employee";
            saveNotificationIfAbsent(
                    managerId,
                    employeeId,
                    "MISSING_CHECKOUT",
                    "Missing checkout detected",
                    employeeName + " had missing checkout on " + dateLabel + " and attendance was system-closed.",
                    Severity.INFO);
        }
    }

    private void saveNotificationIfAbsent(
            Long recipientId,
            Long relatedEmployeeId,
            String type,
            String title,
            String message,
            Severity severity) {
        if (notificationRepository.existsByRecipientEmployeeIdAndRelatedEmployeeIdAndTypeAndCreatedAtAfter(
                recipientId,
                relatedEmployeeId,
                type,
                LocalDateTime.now().minusHours(24))) {
            return;
        }

        notificationRepository.save(Notification.builder()
                .recipientEmployeeId(recipientId)
                .relatedEmployeeId(relatedEmployeeId)
                .title(title)
                .message(message)
                .type(type)
                .severity(severity)
                .read(false)
                .build());
        log.info("Notification created recipientId={} type={} relatedEmployeeId={}", recipientId, type, relatedEmployeeId);
    }

    @Transactional
    public void notifyForOutliers(Long employeeId, Long managerId) {
        if (managerId == null) {
            return;
        }

        List<AttendanceOutlier> openOutliers = attendanceOutlierRepository
                .findByEmployeeIdAndStatus(employeeId, OutlierStatus.OPEN);

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        String employeeName = employee != null ? employee.getName() : "Employee";

        for (AttendanceOutlier outlier : openOutliers) {
            boolean alreadyNotified = notificationRepository
                    .existsByRecipientEmployeeIdAndRelatedEmployeeIdAndTypeAndCreatedAtAfter(
                            managerId,
                            employeeId,
                            outlier.getOutlierType().name(),
                            outlier.getDetectedAt().minusMinutes(1)
                    );
            if (alreadyNotified) {
                continue;
            }

            Notification notification = Notification.builder()
                    .recipientEmployeeId(managerId)
                    .relatedEmployeeId(employeeId)
                    .title("Attendance outlier detected")
                    .message(employeeName + ": " + outlier.getDescription())
                    .type(outlier.getOutlierType().name())
                    .severity(outlier.getSeverity())
                    .read(false)
                    .build();
            notificationRepository.save(notification);
            log.info(
                    "Notification created recipientId={} type={} relatedEmployeeId={}",
                    managerId,
                    outlier.getOutlierType().name(),
                    employeeId);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long employeeId = SecurityUtils.currentUser().getEmployeeId();
        Page<Notification> page = notificationRepository
                .findByRecipientEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);

        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Long employeeId = SecurityUtils.currentUser().getEmployeeId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getRecipientEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("Notification does not belong to current user");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByRecipientEmployeeIdAndReadFalse(
                SecurityUtils.currentUser().getEmployeeId());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientEmployeeId(notification.getRecipientEmployeeId())
                .relatedEmployeeId(notification.getRelatedEmployeeId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .severity(notification.getSeverity())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
