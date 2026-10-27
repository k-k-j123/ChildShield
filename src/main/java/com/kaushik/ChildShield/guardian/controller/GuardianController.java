package com.kaushik.ChildShield.guardian.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.guardian.dto.GuardianDTO;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/health-worker/guardians", "/guardians"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GuardianController {

    private final GuardianService guardianService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private User resolveCurrentUser(HttpSession session) {
        if (session != null && session.getAttribute("currentUser") instanceof User u && u.getId() != null) {
            return userRepository.findById(u.getId()).orElse(u);
        }
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

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
    public ResponseEntity<List<Guardian>> listGuardians(HttpSession session) {
        User user = resolveCurrentUser(session);
        return ResponseEntity.ok(guardianService.findAllForUser(user));
    }

    @PostMapping
    public ResponseEntity<Guardian> createGuardian(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session);
        GuardianDTO dto = parseInput(request, GuardianDTO.class);
        if (dto != null) {
            if (dto.getFullName() == null && request.getParameter("fullName") != null) {
                dto.setFullName(request.getParameter("fullName"));
            }
            if (dto.getPhone() == null && request.getParameter("phone") != null) {
                dto.setPhone(request.getParameter("phone"));
            }
            if (dto.getVillageId() == null && request.getParameter("villageId") != null) {
                try { dto.setVillageId(Long.valueOf(request.getParameter("villageId"))); } catch (Exception ignored) {}
            }
            if (dto.getUsername() == null && request.getParameter("username") != null) {
                dto.setUsername(request.getParameter("username"));
            }
            if (dto.getPassword() == null && request.getParameter("password") != null) {
                dto.setPassword(request.getParameter("password"));
            }
        }
        return ResponseEntity.ok(guardianService.create(dto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guardian> getGuardian(@PathVariable Long id, HttpSession session) {
        User user = resolveCurrentUser(session);
        return ResponseEntity.ok(guardianService.findById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guardian> updateGuardian(@PathVariable Long id, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session);
        GuardianDTO dto = parseInput(request, GuardianDTO.class);
        return ResponseEntity.ok(guardianService.update(id, dto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuardian(@PathVariable Long id, HttpSession session) {
        User user = resolveCurrentUser(session);
        guardianService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
