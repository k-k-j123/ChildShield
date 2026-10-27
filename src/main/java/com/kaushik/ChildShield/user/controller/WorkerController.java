package com.kaushik.ChildShield.user.controller;

import com.kaushik.ChildShield.user.dto.UserDTO;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.service.UserService;
import com.kaushik.ChildShield.village.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping({"/workers", "/healthcare-workers", "/healthcare-workers.html"})
@RequiredArgsConstructor
public class WorkerController {

    private final UserService userService;
    private final VillageService villageService;

    @GetMapping
    public String listWorkers(Model model) {
        model.addAttribute("workers", userService.findByRole(User.Role.HEALTH_WORKER));
        model.addAttribute("userDTO", UserDTO.builder().role(User.Role.HEALTH_WORKER).build());
        model.addAttribute("villages", villageService.findAllActive());
        return "healthcare-workers";
    }

    @PostMapping
    public String createWorker(@ModelAttribute("userDTO") UserDTO userDTO,
                               RedirectAttributes redirectAttributes) {
        userDTO.setRole(User.Role.HEALTH_WORKER);
        userService.create(userDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Health worker created successfully.");
        return "redirect:/healthcare-workers";
    }

    @GetMapping("/{id}/edit")
    public String editWorkerForm(@PathVariable Long id, Model model) {
        User worker = userService.findById(id);
        UserDTO dto = UserDTO.builder()
                .id(worker.getId())
                .fullName(worker.getFullName())
                .username(worker.getUsername())
                .role(worker.getRole())
                .enabled(worker.isEnabled())
                .villageIds(worker.getAssignedVillages().stream().map(v -> v.getId()).collect(java.util.stream.Collectors.toSet()))
                .build();
        model.addAttribute("userDTO", dto);
        model.addAttribute("worker", worker);
        model.addAttribute("villages", villageService.findAllActive());
        return "healthcare-workers";
    }

    @PostMapping("/{id}/edit")
    public String updateWorker(@PathVariable Long id,
                               @ModelAttribute("userDTO") UserDTO userDTO,
                               RedirectAttributes redirectAttributes) {
        userDTO.setRole(User.Role.HEALTH_WORKER);
        userService.update(id, userDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Health worker updated successfully.");
        return "redirect:/healthcare-workers";
    }

    @PostMapping("/{id}/delete")
    public String deleteWorker(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Health worker deactivated successfully.");
        return "redirect:/healthcare-workers";
    }
}
