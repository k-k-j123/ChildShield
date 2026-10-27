package com.kaushik.ChildShield.child.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.child.dto.ChildDTO;
import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.child.service.ChildService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/health-worker/children", "/children"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChildController {

    private final ChildService childService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private User resolveCurrentUser(HttpSession session) {
        return resolveCurrentUser(session, null);
    }

    private User resolveCurrentUser(HttpSession session, HttpServletRequest request) {
        if (session != null && session.getAttribute("currentUser") instanceof User u && u.getId() != null) {
            return userRepository.findById(u.getId()).orElse(u);
        }
        if (request != null) {
            String headerId = request.getHeader("X-User-Id");
            if (headerId != null && !headerId.isBlank()) {
                try {
                    var u = userRepository.findById(Long.valueOf(headerId.trim()));
                    if (u.isPresent()) return u.get();
                } catch (Exception ignored) {}
            }
            String headerUser = request.getHeader("X-User-Username");
            if (headerUser != null && !headerUser.isBlank()) {
                var u = userRepository.findByUsername(headerUser.trim());
                if (u.isPresent()) return u.get();
            }
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
                    } else if (field.getType().equals(LocalDate.class)) {
                        field.set(instance, LocalDate.parse(val));
                    } else if (field.getType().equals(BigDecimal.class)) {
                        field.set(instance, new BigDecimal(val));
                    } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                        field.set(instance, Double.valueOf(val));
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
    public ResponseEntity<List<Child>> listChildren(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        return ResponseEntity.ok(childService.findAllForUser(user));
    }

    @PostMapping
    public ResponseEntity<Child> createChild(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        ChildDTO dto = parseInput(request, ChildDTO.class);
        if (dto != null) {
            if (dto.getGuardianId() == null && request.getParameter("guardianId") != null) {
                try { dto.setGuardianId(Long.valueOf(request.getParameter("guardianId"))); } catch (Exception ignored) {}
            }
            if (dto.getVillageId() == null && request.getParameter("villageId") != null) {
                try { dto.setVillageId(Long.valueOf(request.getParameter("villageId"))); } catch (Exception ignored) {}
            }
            if (dto.getFullName() == null && request.getParameter("fullName") != null) {
                dto.setFullName(request.getParameter("fullName"));
            }
            if (dto.getDateOfBirth() == null && request.getParameter("dateOfBirth") != null) {
                try { dto.setDateOfBirth(LocalDate.parse(request.getParameter("dateOfBirth"))); } catch (Exception ignored) {}
            }
            if (dto.getGender() == null && request.getParameter("gender") != null) {
                dto.setGender(request.getParameter("gender"));
            }
            if (dto.getBirthWeightKg() == null && request.getParameter("birthWeightKg") != null) {
                try { dto.setBirthWeightKg(new BigDecimal(request.getParameter("birthWeightKg"))); } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok(childService.create(dto, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Child> getChild(@PathVariable Long id, HttpSession session) {
        User user = resolveCurrentUser(session);
        return ResponseEntity.ok(childService.findById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Child> updateChild(@PathVariable Long id, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session);
        ChildDTO dto = parseInput(request, ChildDTO.class);
        return ResponseEntity.ok(childService.update(id, dto, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChild(@PathVariable Long id, HttpSession session) {
        User user = resolveCurrentUser(session);
        childService.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
