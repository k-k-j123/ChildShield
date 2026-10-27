package com.kaushik.ChildShield.village.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.village.dto.VillageDTO;
import com.kaushik.ChildShield.village.model.Village;
import com.kaushik.ChildShield.village.service.VillageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/villages", "/villages"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VillageController {

    private final VillageService villageService;
    private final ObjectMapper objectMapper;

    private <T> T parseInput(HttpServletRequest request, Class<T> clazz) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                String line;
                try (var reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                if (sb.length() > 0) {
                    return objectMapper.readValue(sb.toString(), clazz);
                }
            }
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (var field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String val = request.getParameter(field.getName());
                if (val != null) {
                    if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                        field.set(instance, Long.valueOf(val));
                    } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                        field.set(instance, Integer.valueOf(val));
                    } else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                        field.set(instance, Boolean.valueOf(val));
                    } else {
                        field.set(instance, val);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<Village>> listVillages() {
        return ResponseEntity.ok(villageService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<Village> createVillage(HttpServletRequest request) {
        VillageDTO dto = parseInput(request, VillageDTO.class);
        return ResponseEntity.ok(villageService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Village> getVillage(@PathVariable Long id) {
        return ResponseEntity.ok(villageService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Village> updateVillage(@PathVariable Long id, HttpServletRequest request) {
        VillageDTO dto = parseInput(request, VillageDTO.class);
        return ResponseEntity.ok(villageService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVillage(@PathVariable Long id) {
        villageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
