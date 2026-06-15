package com.wfhwfo.attendance.notification.repository;

import com.wfhwfo.attendance.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientEmployeeIdOrderByCreatedAtDesc(Long recipientEmployeeId, Pageable pageable);

    long countByRecipientEmployeeIdAndReadFalse(Long recipientEmployeeId);

    boolean existsByRecipientEmployeeIdAndRelatedEmployeeIdAndTypeAndCreatedAtAfter(
            Long recipientEmployeeId,
            Long relatedEmployeeId,
            String type,
            LocalDateTime createdAtAfter);
}
