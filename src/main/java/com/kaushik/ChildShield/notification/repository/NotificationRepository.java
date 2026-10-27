package com.kaushik.ChildShield.notification.repository;

import com.kaushik.ChildShield.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserIdOrderByScheduledForDesc(Long recipientUserId);
    List<Notification> findAllByOrderByScheduledForDesc();
    boolean existsByReminderKey(String reminderKey);
}
