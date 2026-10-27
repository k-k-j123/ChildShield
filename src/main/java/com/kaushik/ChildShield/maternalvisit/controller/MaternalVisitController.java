package com.kaushik.ChildShield.maternalvisit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushik.ChildShield.maternalvisit.dto.MaternalVisitDTO;
import com.kaushik.ChildShield.maternalvisit.model.MaternalVisit;
import com.kaushik.ChildShield.maternalvisit.repository.MaternalVisitRepository;
import com.kaushik.ChildShield.maternalvisit.service.MaternalVisitService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/maternal-visits", "/maternal-visits"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaternalVisitController {

    private final MaternalVisitService maternalVisitService;
    private final MaternalVisitRepository maternalVisitRepository;
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
                    } else if (field.getType().equals(LocalDate.class)) {
                        field.set(instance, LocalDate.parse(val));
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
    public ResponseEntity<List<MaternalVisit>> listVisits() {
        return ResponseEntity.ok(maternalVisitRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<MaternalVisit> createVisit(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session);
        MaternalVisitDTO dto = parseInput(request, MaternalVisitDTO.class);
        if (dto != null) {
            if (dto.getGuardianId() == null && request.getParameter("guardianId") != null) {
                try {
                    dto.setGuardianId(Long.valueOf(request.getParameter("guardianId")));
                } catch (Exception ignored) {}
            }
            if (dto.getVisitDate() == null && request.getParameter("visitDate") != null) {
                try {
                    dto.setVisitDate(LocalDate.parse(request.getParameter("visitDate")));
                } catch (Exception ignored) {}
            }
            if (dto.getVisitType() == null && request.getParameter("visitType") != null) {
                dto.setVisitType(request.getParameter("visitType"));
            }
            if (dto.getNextVisitDate() == null && request.getParameter("nextVisitDate") != null) {
                try {
                    dto.setNextVisitDate(LocalDate.parse(request.getParameter("nextVisitDate")));
                } catch (Exception ignored) {}
            }
            if (dto.getNotes() == null && request.getParameter("notes") != null) {
                dto.setNotes(request.getParameter("notes"));
            }
        }
        return ResponseEntity.ok(maternalVisitService.create(dto, user));
    }
}
