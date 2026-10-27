package com.kaushik.ChildShield.notification.service;

import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.notification.model.Notification;
import com.kaushik.ChildShield.notification.repository.NotificationRepository;
import com.kaushik.ChildShield.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findForUser(User user) {
        return notificationRepository.findByRecipientUserIdOrderByScheduledForDesc(user.getId());
    }

    @Transactional
    public void markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipientUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
