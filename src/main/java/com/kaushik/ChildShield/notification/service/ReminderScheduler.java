package com.kaushik.ChildShield.notification.service;

import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.child.repository.ChildRepository;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.notification.model.Notification;
import com.kaushik.ChildShield.notification.repository.NotificationRepository;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ChildVaccinationRepository childVaccinationRepository;
    private final NotificationRepository notificationRepository;
    private final WhatsAppService whatsAppService;
    private final ChildRepository childRepository;
    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Scheduled(cron = "0 0 6 * * *") // Daily morning batch at 6 AM
    @Scheduled(fixedDelay = 3600000, initialDelay = 10000) // 10s after startup and every hour
    @Transactional
    public void generateReminders() {
        log.info("Running automatic vaccination reminder generator...");
        Map<String, Integer> overdueResult = sendOverdueRemindersWithResult();
        int dueToday = sendDueTodayReminders();
        log.info("Automatic reminder generator finished. Overdue in-app: {}, WhatsApp: {}, Due today: {}",
                overdueResult.get("inAppCount"), overdueResult.get("whatsappCount"), dueToday);
    }

    public User ensureUserForGuardian(Guardian guardian) {
        if (guardian == null) return null;
        if (guardian.getUser() != null) {
            return guardian.getUser();
        }
        String cleanPhone = guardian.getPhone() != null ? guardian.getPhone().replaceAll("\\D", "") : "";
        String baseUsername = !cleanPhone.isEmpty() ? "mother_" + cleanPhone : ("user_" + guardian.getId() + "_" + System.currentTimeMillis());
        String candidateUsername = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(candidateUsername)) {
            candidateUsername = baseUsername + counter++;
        }
        String rawPassword = (cleanPhone.length() >= 4) ? cleanPhone : "mother123";
        User newUser = User.builder()
                .fullName(guardian.getFullName() != null && !guardian.getFullName().isBlank() ? guardian.getFullName().trim() : "Guardian")
                .username(candidateUsername)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(User.Role.PARENT)
                .enabled(true)
                .build();
        newUser = userRepository.save(newUser);
        guardian.setUser(newUser);
        guardianRepository.save(guardian);
        return newUser;
    }

    @Transactional
    public Map<String, Integer> sendOverdueRemindersWithResult() {
        LocalDate today = LocalDate.now();
        List<ChildVaccination> vaccinations = childVaccinationRepository.findAllWithDetails();
        int inAppCount = 0;
        int whatsappCount = 0;

        Map<Long, List<ChildVaccination>> overdueByChild = new LinkedHashMap<>();

        for (ChildVaccination v : vaccinations) {
            if (v.getStatus() == ChildVaccination.Status.COMPLETED || v.getStatus() == ChildVaccination.Status.CANCELLED) {
                continue;
            }

            var child = v.getChild();
            if (child == null || child.getGuardian() == null) {
                continue;
            }
            var guardian = child.getGuardian();
            var recipient = ensureUserForGuardian(guardian);
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
                    inAppCount++;
                }

                overdueByChild.computeIfAbsent(child.getId(), k -> new ArrayList<>()).add(v);
            }
        }

        // Dispatch WhatsApp notifications per family
        for (List<ChildVaccination> childDoses : overdueByChild.values()) {
            if (childDoses.isEmpty()) continue;
            var child = childDoses.get(0).getChild();
            var guardian = child.getGuardian();
            if (guardian != null && guardian.getPhone() != null && !guardian.getPhone().isBlank()) {
                List<Map<String, String>> doseList = new ArrayList<>();
                for (var cv : childDoses) {
                    String dName = cv.getScheduleDose() != null ? cv.getScheduleDose().getDisplayName() : "Vaccine Dose";
                    String dDate = cv.getDueDate() != null ? cv.getDueDate().toString() : "";
                    doseList.add(Map.of("name", dName, "dueDate", dDate));
                }

                boolean sent = whatsAppService.sendOverdueVaccinationAlert(
                        guardian.getPhone(),
                        guardian.getFullName(),
                        child.getFullName(),
                        doseList
                );
                if (sent) {
                    whatsappCount++;
                }
            }
        }

        return Map.of("inAppCount", inAppCount, "whatsappCount", whatsappCount);
    }

    @Transactional
    public int sendOverdueReminders() {
        return sendOverdueRemindersWithResult().getOrDefault("inAppCount", 0);
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
            var recipient = ensureUserForGuardian(guardian);
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

    @Transactional
    public Map<String, Object> sendRemindersForChild(Long childId) {
        if (childId == null) {
            return Map.of("success", false, "message", "Child ID is required.");
        }
        Optional<Child> childOpt = childRepository.findById(childId);
        if (childOpt.isEmpty()) {
            return Map.of("success", false, "message", "Child not found with ID: " + childId);
        }
        Child child = childOpt.get();
        Guardian guardian = child.getGuardian();
        if (guardian == null) {
            return Map.of("success", false, "message", "No guardian assigned to child " + (child.getFullName() != null ? child.getFullName() : ""));
        }

        User recipient = ensureUserForGuardian(guardian);

        LocalDate today = LocalDate.now();
        List<ChildVaccination> vaccinations = childVaccinationRepository.findByChildIdWithDetails(childId);
        List<ChildVaccination> overdueDoses = new ArrayList<>();
        List<ChildVaccination> dueTodayDoses = new ArrayList<>();

        int inAppCount = 0;

        for (ChildVaccination v : vaccinations) {
            if (v.getStatus() == ChildVaccination.Status.COMPLETED || v.getStatus() == ChildVaccination.Status.CANCELLED) {
                continue;
            }

            LocalDate due = v.getDueDate();
            LocalDate dob = child.getDateOfBirth();

            // Newborn grace period for automatic overdue: 14 days
            boolean isBirthDose = (dob != null && due != null && due.isEqual(dob));
            boolean overdueGrace = isBirthDose && !dob.isBefore(today.minusDays(14));

            if (due != null && due.isBefore(today) && !overdueGrace) {
                overdueDoses.add(v);
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
                    inAppCount++;
                }
            } else if (due != null && (due.isEqual(today) || (isBirthDose && overdueGrace))) {
                dueTodayDoses.add(v);
                String reminderKey = "DUE_TODAY_" + v.getId() + "_" + today;
                if (!notificationRepository.existsByReminderKey(reminderKey)) {
                    String doseName = v.getScheduleDose() != null ? v.getScheduleDose().getDisplayName() : "Vaccine Dose";
                    String childName = child.getFullName() != null ? child.getFullName() : "your child";
                    notificationRepository.save(Notification.builder()
                            .recipientUser(recipient)
                            .childVaccination(v)
                            .notificationType(Notification.NotificationType.UPCOMING)
                            .message("Alert: Vaccine dose '" + doseName + "' for " + childName + " is due TODAY (" + due + ").")
                            .scheduledFor(LocalDateTime.now())
                            .reminderKey(reminderKey)
                            .build());
                    inAppCount++;
                }
            }
        }

        boolean whatsappSent = false;
        String guardianPhone = guardian.getPhone();

        if (!overdueDoses.isEmpty()) {
            if (guardianPhone != null && !guardianPhone.isBlank()) {
                List<Map<String, String>> doseList = new ArrayList<>();
                for (var cv : overdueDoses) {
                    String dName = cv.getScheduleDose() != null ? cv.getScheduleDose().getDisplayName() : "Vaccine Dose";
                    String dDate = cv.getDueDate() != null ? cv.getDueDate().toString() : "";
                    doseList.add(Map.of("name", dName, "dueDate", dDate));
                }
                whatsappSent = whatsAppService.sendOverdueVaccinationAlert(
                        guardianPhone,
                        guardian.getFullName(),
                        child.getFullName(),
                        doseList
                );
            }
            String childName = child.getFullName() != null ? child.getFullName() : "Child";
            String msg = "Sent reminder for " + childName + ": " + overdueDoses.size() + " overdue dose(s). ";
            if (whatsappSent) {
                msg += "WhatsApp alert dispatched to " + guardianPhone + ".";
            } else if (guardianPhone == null || guardianPhone.isBlank()) {
                msg += "Guardian has no phone number recorded.";
            } else {
                msg += "In-app alert recorded (WhatsApp simulation/fallback).";
            }

            return Map.of(
                    "success", true,
                    "childId", childId,
                    "childName", childName,
                    "guardianName", guardian.getFullName() != null ? guardian.getFullName() : "",
                    "phone", guardianPhone != null ? guardianPhone : "",
                    "overdueCount", overdueDoses.size(),
                    "dueTodayCount", dueTodayDoses.size(),
                    "inAppCount", inAppCount,
                    "whatsappSent", whatsappSent,
                    "message", msg
            );
        } else if (!dueTodayDoses.isEmpty()) {
            String childName = child.getFullName() != null ? child.getFullName() : "Child";
            return Map.of(
                    "success", true,
                    "childId", childId,
                    "childName", childName,
                    "guardianName", guardian.getFullName() != null ? guardian.getFullName() : "",
                    "phone", guardianPhone != null ? guardianPhone : "",
                    "overdueCount", 0,
                    "dueTodayCount", dueTodayDoses.size(),
                    "inAppCount", inAppCount,
                    "whatsappSent", false,
                    "message", "Notification recorded: " + childName + " has " + dueTodayDoses.size() + " dose(s) due today."
            );
        } else {
            String childName = child.getFullName() != null ? child.getFullName() : "Child";
            return Map.of(
                    "success", true,
                    "childId", childId,
                    "childName", childName,
                    "overdueCount", 0,
                    "dueTodayCount", 0,
                    "inAppCount", 0,
                    "whatsappSent", false,
                    "message", childName + " has no overdue vaccinations. All doses are currently up to date!"
            );
        }
    }
}
