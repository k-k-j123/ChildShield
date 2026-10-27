package com.kaushik.ChildShield.dashboard.controller;

import com.kaushik.ChildShield.child.repository.ChildRepository;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.village.repository.VillageRepository;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final VillageRepository villageRepository;
    private final GuardianRepository guardianRepository;
    private final ChildRepository childRepository;
    private final ChildVaccinationRepository childVaccinationRepository;

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = resolveUser(userDetails);
        model.addAttribute("currentUser", user);

        if (user.getRole() == User.Role.ADMIN) {
            model.addAttribute("totalVillages", villageRepository.count());
            model.addAttribute("totalWorkers", userRepository.findByRole(User.Role.HEALTH_WORKER).size());
            model.addAttribute("totalGuardians", guardianRepository.count());
            model.addAttribute("totalChildren", childRepository.count());
            return "admin-dashboard";
        } else if (user.getRole() == User.Role.HEALTH_WORKER) {
            var villages = user.getAssignedVillages();
            model.addAttribute("assignedVillages", villages);
            model.addAttribute("totalGuardians", guardianRepository.findByVillageIn(List.copyOf(villages)).size());
            model.addAttribute("totalChildren", childRepository.findByVillageIn(List.copyOf(villages)).size());
            return "health-worker-dashboard";
        } else if (user.getRole() == User.Role.PARENT) {
            var guardian = guardianRepository.findByUserId(user.getId()).orElse(null);
            model.addAttribute("guardian", guardian);
            if (guardian != null) {
                var children = childRepository.findByGuardianId(guardian.getId());
                model.addAttribute("children", children);
            }
            return "user-dashboard";
        }
        return "user-dashboard";
    }

    @GetMapping({"/admin-dashboard", "/admin-dashboard.html"})
    public String adminDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return dashboard(userDetails, model);
    }

    @GetMapping({"/health-worker-dashboard", "/health-worker-dashboard.html"})
    public String healthWorkerDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return dashboard(userDetails, model);
    }

    @GetMapping({"/user-dashboard", "/user-dashboard.html"})
    public String userDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return dashboard(userDetails, model);
    }

    @GetMapping({"/vaccination-schedule", "/vaccination-schedule.html"})
    public String vaccinationSchedule(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "vaccination-schedule";
    }

    @GetMapping({"/visit-records", "/visit-records.html"})
    public String visitRecords(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "visit-records";
    }

    @GetMapping({"/reports", "/reports.html"})
    public String reports(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "reports";
    }

    @GetMapping({"/profile", "/profile.html"})
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "profile";
    }

    @GetMapping({"/beneficiaries", "/beneficiaries.html"})
    public String beneficiaries(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "beneficiaries";
    }

    @GetMapping({"/healthcare-workers", "/healthcare-workers.html"})
    public String healthcareWorkers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "healthcare-workers";
    }

    @GetMapping({"/notifications", "/notifications.html"})
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) model.addAttribute("currentUser", resolveUser(userDetails));
        return "notifications";
    }
}
