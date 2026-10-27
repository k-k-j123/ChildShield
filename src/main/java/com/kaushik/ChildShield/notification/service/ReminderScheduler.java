package com.kaushik.ChildShield.notification.service;

import com.kaushik.ChildShield.notification.model.Notification;
import com.kaushik.ChildShield.notification.repository.NotificationRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ChildVaccinationRepository childVaccinationRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 6 * * *") // Run daily at 6 AM
    @Transactional
    public void generateReminders() {
        log.info("Running daily vaccination reminder generator...");
        LocalDate today = LocalDate.now();

        List<ChildVaccination> vaccinations = childVaccinationRepository.findAll();

        for (ChildVaccination v : vaccinations) {
            if (v.getStatus() == ChildVaccination.Status.COMPLETED || v.getStatus() == ChildVaccination.Status.CANCELLED) {
                continue;
            }

            var guardian = v.getChild().getGuardian();
            if (guardian.getUser() == null) {
                continue;
            }

            var recipient = guardian.getUser();
            LocalDate due = v.getDueDate();

            if (due.isEqual(today)) {
                String reminderKey = "DUE_TODAY_" + v.getId();
                if (!notificationRepository.existsByReminderKey(reminderKey)) {
                    notificationRepository.save(Notification.builder()
                            .recipientUser(recipient)
                            .childVaccination(v)
                            .notificationType(Notification.NotificationType.UPCOMING)
                            .message("Vaccine dose '" + v.getScheduleDose().getDisplayName() + "' for your child " + v.getChild().getFullName() + " is due TODAY.")
                            .scheduledFor(LocalDateTime.now())
                            .reminderKey(reminderKey)
                            .build());
                }
            } else if (due.isBefore(today)) {
                String reminderKey = "OVERDUE_" + v.getId();
                if (!notificationRepository.existsByReminderKey(reminderKey)) {
                    notificationRepository.save(Notification.builder()
                            .recipientUser(recipient)
                            .childVaccination(v)
                            .notificationType(Notification.NotificationType.OVERDUE)
                            .message("Vaccine dose '" + v.getScheduleDose().getDisplayName() + "' for your child " + v.getChild().getFullName() + " is OVERDUE (due on " + due + ").")
                            .scheduledFor(LocalDateTime.now())
                            .reminderKey(reminderKey)
                            .build());
                }
            }
        }
    }
}
