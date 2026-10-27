package com.kaushik.ChildShield.notification.controller;

import com.kaushik.ChildShield.notification.model.Notification;
import com.kaushik.ChildShield.notification.repository.NotificationRepository;
import com.kaushik.ChildShield.notification.service.NotificationService;
import com.kaushik.ChildShield.notification.service.ReminderScheduler;
import com.kaushik.ChildShield.notification.service.WhatsAppService;
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
    private final WhatsAppService whatsAppService;

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
        Map<String, Integer> result = reminderScheduler.sendOverdueRemindersWithResult();
        int inApp = result.getOrDefault("inAppCount", 0);
        int whatsapp = result.getOrDefault("whatsappCount", 0);

        String msg;
        if (inApp > 0 || whatsapp > 0) {
            msg = "Dispatched " + inApp + " in-app reminder(s) and " + whatsapp + " WhatsApp alert(s) to registered mothers/guardians.";
        } else {
            msg = "All vaccination records are up to date! No pending overdue reminders.";
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "remindersSent", inApp,
                "whatsappSent", whatsapp,
                "message", msg
        ));
    }

    @PostMapping("/child/{childId}/send-reminders")
    public ResponseEntity<Map<String, Object>> sendRemindersForChild(
            @PathVariable Long childId,
            HttpServletRequest request,
            HttpSession session) {
        User user = resolveCurrentUser(session, request);
        Map<String, Object> result = reminderScheduler.sendRemindersForChild(childId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/whatsapp/status")
    public ResponseEntity<Map<String, Object>> getWhatsAppStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", whatsAppService.isEnabled(),
                "configured", whatsAppService.isConfigured(),
                "messageType", whatsAppService.getMessageType(),
                "templateName", whatsAppService.getTemplateName()
        ));
    }

    @PostMapping("/whatsapp/test")
    public ResponseEntity<Map<String, Object>> sendTestWhatsApp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phone number is required"));
        }

        String type = body.getOrDefault("type", whatsAppService.getMessageType());
        boolean sent;
        String tName = body.getOrDefault("template", whatsAppService.getTemplateName());

        if ("text".equalsIgnoreCase(type)) {
            String message = body.getOrDefault("message", "Hello from ChildShield! This is a test notification.");
            sent = whatsAppService.sendTextMessage(phone, message);
        } else {
            String p1 = body.getOrDefault("param1", "Guardian / Parent");
            String p2 = body.getOrDefault("param2", "BCG, OPV 0, HepB-1");
            String p3 = body.getOrDefault("param3", "Health Center Visit Due");
            sent = whatsAppService.sendTemplateMessage(phone, tName, whatsAppService.getTemplateLanguage(), List.of(p1, p2, p3));
        }

        return ResponseEntity.ok(Map.of(
                "success", sent,
                "type", type,
                "templateUsed", "template".equalsIgnoreCase(type) ? tName : "none (text)",
                "message", sent
                        ? "Test WhatsApp notification dispatched successfully using " + ("template".equalsIgnoreCase(type) ? "template '" + tName + "'" : "text") + "!"
                        : "Failed to send WhatsApp message. Please check logs and API configuration."
        ));
    }
}
