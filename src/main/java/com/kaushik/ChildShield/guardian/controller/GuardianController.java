package com.kaushik.ChildShield.guardian.controller;

import com.kaushik.ChildShield.guardian.dto.GuardianDTO;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.village.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;
    private final VillageService villageService;
    private final UserRepository userRepository;

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    @GetMapping
    public String listGuardians(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = resolveUser(userDetails);
        model.addAttribute("guardians", guardianService.findAllForUser(user));
        model.addAttribute("guardianDTO", new GuardianDTO());
        model.addAttribute("villages", villageService.findAllActive());
        model.addAttribute("users", userRepository.findByRole(User.Role.PARENT));
        model.addAttribute("currentUser", user);
        return "guardians/list";
    }

    @PostMapping
    public String createGuardian(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute("guardianDTO") GuardianDTO guardianDTO,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        guardianService.create(guardianDTO, user);
        redirectAttributes.addFlashAttribute("successMessage", "Guardian registered successfully.");
        return "redirect:/guardians";
    }

    @GetMapping("/{id}/edit")
    public String editGuardianForm(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model) {
        User user = resolveUser(userDetails);
        Guardian guardian = guardianService.findById(id, user);
        GuardianDTO dto = GuardianDTO.builder()
                .id(guardian.getId())
                .fullName(guardian.getFullName())
                .phone(guardian.getPhone())
                .villageId(guardian.getVillage().getId())
                .userId(guardian.getUser() != null ? guardian.getUser().getId() : null)
                .build();
        model.addAttribute("guardianDTO", dto);
        model.addAttribute("guardian", guardian);
        model.addAttribute("villages", villageService.findAllActive());
        model.addAttribute("users", userRepository.findByRole(User.Role.PARENT));
        return "guardians/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateGuardian(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute("guardianDTO") GuardianDTO guardianDTO,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        guardianService.update(id, guardianDTO, user);
        redirectAttributes.addFlashAttribute("successMessage", "Guardian updated successfully.");
        return "redirect:/guardians";
    }

    @PostMapping("/{id}/delete")
    public String deleteGuardian(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        guardianService.delete(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Guardian deleted successfully.");
        return "redirect:/guardians";
    }
}
