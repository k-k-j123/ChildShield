package com.kaushik.ChildShield.child.controller;

import com.kaushik.ChildShield.child.dto.ChildDTO;
import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.child.service.ChildService;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.village.service.VillageService;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;
    private final GuardianService guardianService;
    private final VillageService villageService;
    private final UserRepository userRepository;
    private final ChildVaccinationRepository childVaccinationRepository;

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    @GetMapping
    public String listChildren(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = resolveUser(userDetails);
        model.addAttribute("children", childService.findAllForUser(user));
        model.addAttribute("childDTO", new ChildDTO());
        model.addAttribute("guardians", guardianService.findAllForUser(user));
        model.addAttribute("villages", villageService.findAllActive());
        model.addAttribute("currentUser", user);
        return "children/list";
    }

    @PostMapping
    public String createChild(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute("childDTO") ChildDTO childDTO,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        childService.create(childDTO, user);
        redirectAttributes.addFlashAttribute("successMessage", "Child registered and immunization schedule generated successfully.");
        return "redirect:/children";
    }

    @GetMapping("/{id}")
    public String childDetails(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User user = resolveUser(userDetails);
        Child child = childService.findById(id, user);
        model.addAttribute("child", child);
        model.addAttribute("vaccinations", childVaccinationRepository.findByChildId(child.getId()));
        model.addAttribute("currentUser", user);
        return "children/details";
    }

    @GetMapping("/{id}/edit")
    public String editChildForm(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = resolveUser(userDetails);
        Child child = childService.findById(id, user);
        ChildDTO dto = ChildDTO.builder()
                .id(child.getId())
                .guardianId(child.getGuardian().getId())
                .fullName(child.getFullName())
                .dateOfBirth(child.getDateOfBirth())
                .gender(child.getGender())
                .birthWeightKg(child.getBirthWeightKg())
                .villageId(child.getVillage().getId())
                .build();
        model.addAttribute("childDTO", dto);
        model.addAttribute("child", child);
        model.addAttribute("guardians", guardianService.findAllForUser(user));
        model.addAttribute("villages", villageService.findAllActive());
        return "children/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateChild(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute("childDTO") ChildDTO childDTO,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        childService.update(id, childDTO, user);
        redirectAttributes.addFlashAttribute("successMessage", "Child updated successfully.");
        return "redirect:/children/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteChild(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        childService.delete(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Child record deleted successfully.");
        return "redirect:/children";
    }
}
