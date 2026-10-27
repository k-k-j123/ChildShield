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

    @Scheduled(cron = "0 0 6 * * *") // Daily morning batch at 6 AM
    @Scheduled(fixedDelay = 3600000, initialDelay = 10000) // 10s after startup and every hour
    @Transactional
    public void generateReminders() {
        log.info("Running automatic vaccination reminder generator...");
        int overdue = sendOverdueReminders();
        int dueToday = sendDueTodayReminders();
        log.info("Automatic reminder generator finished. Overdue sent: {}, Due today sent: {}", overdue, dueToday);
    }

    @Transactional
    public int sendOverdueReminders() {
        LocalDate today = LocalDate.now();
        List<ChildVaccination> vaccinations = childVaccinationRepository.findAllWithDetails();
        int count = 0;

        for (ChildVaccination v : vaccinations) {
            if (v.getStatus() == ChildVaccination.Status.COMPLETED || v.getStatus() == ChildVaccination.Status.CANCELLED) {
                continue;
            }

            var child = v.getChild();
            if (child == null || child.getGuardian() == null) {
                continue;
            }
            var guardian = child.getGuardian();
            var recipient = guardian.getUser();
            if (recipient == null) {
                continue;
            }

            LocalDate due = v.getDueDate();
            LocalDate dob = child.getDateOfBirth();

            // For newborn birth doses, grant a 14-day window before sending overdue alert
            if (dob != null && due != null && due.isEqual(dob) && !dob.isBefore(today.minusDays(14))) {
                continue;
            }

            if (due != null && due.isBefore(today)) {
                String reminderKey = "OVERDUE_" + v.getId() + "_" + today;
                if (!notificationRepository.existsByReminderKey(reminderKey)) {
                    String doseName = v.getScheduleDose() != null ? v.getScheduleDose().getDisplayName() : "Vaccine Dose";
                    String childName = child.getFullName() != null ? child.getFullName() : "your child";
                    notificationRepository.save(Notification.builder()
                            .recipientUser(recipient)
                            .childVaccination(v)
                            .notificationType(Notification.NotificationType.OVERDUE)
                            .message("Reminder: Vaccine dose '" + doseName + "' for " + childName + " is OVERDUE (due on " + due + "). Please visit the health center soon.")
                            .scheduledFor(LocalDateTime.now())
                            .reminderKey(reminderKey)
                            .build());
                    count++;
                }
            }
        }
        return count;
    }

    @Transactional
    public int sendDueTodayReminders() {
        LocalDate today = LocalDate.now();
        List<ChildVaccination> vaccinations = childVaccinationRepository.findAllWithDetails();
        int count = 0;

        for (ChildVaccination v : vaccinations) {
            if (v.getStatus() == ChildVaccination.Status.COMPLETED || v.getStatus() == ChildVaccination.Status.CANCELLED) {
                continue;
            }

            var child = v.getChild();
            if (child == null || child.getGuardian() == null) {
                continue;
            }
            var guardian = child.getGuardian();
            var recipient = guardian.getUser();
            if (recipient == null) {
                continue;
            }

            LocalDate due = v.getDueDate();
            if (due != null && due.isEqual(today)) {
                String reminderKey = "DUE_TODAY_" + v.getId() + "_" + today;
                if (!notificationRepository.existsByReminderKey(reminderKey)) {
                    String doseName = v.getScheduleDose() != null ? v.getScheduleDose().getDisplayName() : "Vaccine Dose";
                    String childName = child.getFullName() != null ? child.getFullName() : "your child";
                    notificationRepository.save(Notification.builder()
                            .recipientUser(recipient)
                            .childVaccination(v)
                            .notificationType(Notification.NotificationType.UPCOMING)
                            .message("Alert: Vaccine dose '" + doseName + "' for " + childName + " is due TODAY (" + today + ").")
                            .scheduledFor(LocalDateTime.now())
                            .reminderKey(reminderKey)
                            .build());
                    count++;
                }
            }
        }
        return count;
    }
}
