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

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        model.addAttribute("currentUser", user);

        if (user.getRole() == User.Role.ADMIN) {
            model.addAttribute("totalVillages", villageRepository.count());
            model.addAttribute("totalWorkers", userRepository.findByRole(User.Role.HEALTH_WORKER).size());
            model.addAttribute("totalGuardians", guardianRepository.count());
            model.addAttribute("totalChildren", childRepository.count());
        } else if (user.getRole() == User.Role.HEALTH_WORKER) {
            var villages = user.getAssignedVillages();
            model.addAttribute("assignedVillages", villages);
            model.addAttribute("totalGuardians", guardianRepository.findByVillageIn(List.copyOf(villages)).size());
            model.addAttribute("totalChildren", childRepository.findByVillageIn(List.copyOf(villages)).size());
        } else if (user.getRole() == User.Role.PARENT) {
            var guardian = guardianRepository.findByUserId(user.getId()).orElse(null);
            model.addAttribute("guardian", guardian);
            if (guardian != null) {
                var children = childRepository.findByGuardianId(guardian.getId());
                model.addAttribute("children", children);
            }
        }

        return "dashboard";
    }
}
