package com.kaushik.ChildShield.maternalvisit.controller;

import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.maternalvisit.dto.MaternalVisitDTO;
import com.kaushik.ChildShield.maternalvisit.service.MaternalVisitService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/maternal-visits")
@RequiredArgsConstructor
public class MaternalVisitController {

    private final MaternalVisitService maternalVisitService;
    private final GuardianRepository guardianRepository;
    private final GuardianService guardianService;
    private final UserRepository userRepository;

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

    @GetMapping("/guardian/{guardianId}")
    public String listForGuardian(@PathVariable Long guardianId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User user = resolveUser(userDetails);
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new IllegalStateException("Guardian not found"));
        guardianService.validateAccess(guardian.getVillage(), user);

        model.addAttribute("guardian", guardian);
        model.addAttribute("visits", maternalVisitService.findByGuardian(guardianId, user));
        model.addAttribute("maternalVisitDTO", MaternalVisitDTO.builder().guardianId(guardianId).build());
        model.addAttribute("currentUser", user);
        return "maternalvisits/list";
    }

    @PostMapping
    public String createVisit(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute("maternalVisitDTO") MaternalVisitDTO dto,
                              RedirectAttributes redirectAttributes) {
        User user = resolveUser(userDetails);
        maternalVisitService.create(dto, user);
        redirectAttributes.addFlashAttribute("successMessage", "Maternal health visit recorded successfully.");
        return "redirect:/maternal-visits/guardian/" + dto.getGuardianId();
    }
}
