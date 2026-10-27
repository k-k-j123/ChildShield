package com.kaushik.ChildShield.vaccination.controller;

import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/vaccinations")
@RequiredArgsConstructor
public class VaccinationController {

    private final ChildVaccinationRepository childVaccinationRepository;
    private final GuardianService guardianService;
    private final UserRepository userRepository;

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    @PostMapping("/{id}/administer")
    @Transactional
    public String administerDose(@PathVariable Long id,
                                 @RequestParam(required = false) LocalDate administeredDate,
                                 @RequestParam(required = false) String notes,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        ChildVaccination vaccination = childVaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination record not found"));

        guardianService.validateAccess(vaccination.getChild().getVillage(), user);

        vaccination.setStatus(ChildVaccination.Status.COMPLETED);
        vaccination.setAdministeredDate(administeredDate != null ? administeredDate : LocalDate.now());
        vaccination.setRecordedBy(user);
        vaccination.setNotes(notes);

        childVaccinationRepository.save(vaccination);

        redirectAttributes.addFlashAttribute("successMessage", "Vaccination dose marked as administered successfully.");
        return "redirect:/children/" + vaccination.getChild().getId();
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public String cancelDose(@PathVariable Long id,
                             @RequestParam(required = false) String notes,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        ChildVaccination vaccination = childVaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination record not found"));

        guardianService.validateAccess(vaccination.getChild().getVillage(), user);

        vaccination.setStatus(ChildVaccination.Status.CANCELLED);
        vaccination.setRecordedBy(user);
        vaccination.setNotes(notes);

        childVaccinationRepository.save(vaccination);

        redirectAttributes.addFlashAttribute("successMessage", "Vaccination dose cancelled.");
        return "redirect:/children/" + vaccination.getChild().getId();
    }
}
