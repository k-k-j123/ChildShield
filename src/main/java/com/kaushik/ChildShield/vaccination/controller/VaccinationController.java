package com.kaushik.ChildShield.vaccination.controller;

import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.child.repository.ChildRepository;
import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/vaccinations", "/vaccinations"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VaccinationController {

    private final ChildVaccinationRepository childVaccinationRepository;
    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final ChildRepository childRepository;

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

    @GetMapping
    public ResponseEntity<List<ChildVaccination>> listVaccinations(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        if (user != null && user.getRole() == User.Role.PARENT) {
            Guardian guardian = guardianRepository.findByUserId(user.getId()).orElse(null);
            if (guardian == null) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(childVaccinationRepository.findByGuardianIdWithDetails(guardian.getId()));
        } else if (user != null && user.getRole() == User.Role.HEALTH_WORKER) {
            if (user.getAssignedVillages() != null && !user.getAssignedVillages().isEmpty()) {
                return ResponseEntity.ok(childVaccinationRepository.findByVillagesWithDetails(List.copyOf(user.getAssignedVillages())));
            }
        }
        return ResponseEntity.ok(childVaccinationRepository.findAllWithDetails());
    }

    @GetMapping("/child/{childId}")
    public ResponseEntity<List<ChildVaccination>> getVaccinationsForChild(@PathVariable Long childId, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(session, request);
        if (user != null && user.getRole() == User.Role.PARENT) {
            Guardian guardian = guardianRepository.findByUserId(user.getId()).orElse(null);
            Child child = childRepository.findById(childId).orElse(null);
            if (guardian == null || child == null || !child.getGuardian().getId().equals(guardian.getId())) {
                throw new AccessDeniedException("Access denied to child records");
            }
        }
        return ResponseEntity.ok(childVaccinationRepository.findByChildIdWithDetails(childId));
    }

    @PostMapping("/{id}/administer")
    @Transactional
    public ResponseEntity<ChildVaccination> administerDose(@PathVariable Long id,
                                                           @RequestBody(required = false) Map<String, Object> body,
                                                           @RequestParam(required = false) LocalDate administeredDate,
                                                           @RequestParam(required = false) String notes,
                                                           HttpServletRequest request,
                                                           HttpSession session) {
        User user = resolveCurrentUser(session, request);
        ChildVaccination vaccination = childVaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination record not found"));

        LocalDate dateToUse = administeredDate;
        String notesToUse = notes;

        if (body != null) {
            if (body.get("administeredDate") != null) {
                try {
                    dateToUse = LocalDate.parse(body.get("administeredDate").toString());
                } catch (Exception ignored) {
                }
            }
            if (body.get("notes") != null) {
                notesToUse = body.get("notes").toString();
            }
        }

        vaccination.setStatus(ChildVaccination.Status.COMPLETED);
        vaccination.setAdministeredDate(dateToUse != null ? dateToUse : LocalDate.now());
        vaccination.setRecordedBy(user);
        vaccination.setNotes(notesToUse);

        return ResponseEntity.ok(childVaccinationRepository.save(vaccination));
    }
}
