package com.kaushik.ChildShield.village.controller;

import com.kaushik.ChildShield.village.dto.VillageDTO;
import com.kaushik.ChildShield.village.model.Village;
import com.kaushik.ChildShield.village.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/villages")
@RequiredArgsConstructor
public class VillageController {

    private final VillageService villageService;

    @GetMapping
    public String listVillages(Model model) {
        model.addAttribute("villages", villageService.findAllActive());
        model.addAttribute("villageDTO", new VillageDTO());
        return "villages/list";
    }

    @PostMapping
    public String createVillage(@ModelAttribute("villageDTO") VillageDTO villageDTO,
                                RedirectAttributes redirectAttributes) {
        villageService.create(villageDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Village created successfully.");
        return "redirect:/villages";
    }

    @GetMapping("/{id}/edit")
    public String editVillageForm(@PathVariable Long id, Model model) {
        Village village = villageService.findById(id);
        VillageDTO dto = VillageDTO.builder()
                .id(village.getId())
                .name(village.getName())
                .district(village.getDistrict())
                .state(village.getState())
                .active(village.isActive())
                .build();
        model.addAttribute("villageDTO", dto);
        model.addAttribute("village", village);
        return "villages/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateVillage(@PathVariable Long id,
                                @ModelAttribute("villageDTO") VillageDTO villageDTO,
                                RedirectAttributes redirectAttributes) {
        villageService.update(id, villageDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Village updated successfully.");
        return "redirect:/villages";
    }

    @PostMapping("/{id}/delete")
    public String deleteVillage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        villageService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Village deactivated successfully.");
        return "redirect:/villages";
    }
}
