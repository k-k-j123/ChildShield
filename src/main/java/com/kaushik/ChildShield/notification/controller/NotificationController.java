package com.kaushik.ChildShield.notification.controller;

import com.kaushik.ChildShield.notification.model.Notification;
import com.kaushik.ChildShield.notification.repository.NotificationRepository;
import com.kaushik.ChildShield.notification.service.NotificationService;
import com.kaushik.ChildShield.notification.service.ReminderScheduler;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/notifications", "/notifications"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ReminderScheduler reminderScheduler;

    private User resolveCurrentUser(HttpSession session, HttpServletRequest request) {
        if (session != null && session.getAttribute("currentUser") instanceof User u && u.getId() != null) {
            return userRepository.findById(u.getId()).orElse(u);
        }
        if (request != null) {
            String headerId = request.getHeader("X-User-Id");
            if (headerId != null && !headerId.isBlank()) {
                try {
                    var u = userRepository.findById(Long.valueOf(headerId.trim()));
                    if (u.isPresent()) return u.get();
                } catch (Exception ignored) {}
            }
            String headerUser = request.getHeader("X-User-Username");
            if (headerUser != null && !headerUser.isBlank()) {
                var u = userRepository.findByUsername(headerUser.trim());
                if (u.isPresent()) return u.get();
            }
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<Notification>> listNotifications(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        if (user != null && user.getRole() == User.Role.PARENT) {
            return ResponseEntity.ok(notificationRepository.findByRecipientUserIdOrderByScheduledForDesc(user.getId()));
        }
        return ResponseEntity.ok(notificationRepository.findAllByOrderByScheduledForDesc());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        notificationService.markAsRead(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/send-overdue-reminders", "/trigger-reminders"})
    public ResponseEntity<Map<String, Object>> sendOverdueReminders(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        int count = reminderScheduler.sendOverdueReminders();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "remindersSent", count,
                "message", count > 0
                        ? "Successfully sent " + count + " overdue vaccination reminder(s) to registered mothers/guardians."
                        : "All vaccination records are up to date! No pending overdue reminders."
        ));
    }
}
