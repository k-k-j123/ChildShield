package com.kaushik.ChildShield.vaccination.controller;

import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private User resolveDefaultUser() {
        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<ChildVaccination>> listVaccinations() {
        return ResponseEntity.ok(childVaccinationRepository.findAllWithDetails());
    }

    @GetMapping("/child/{childId}")
    public ResponseEntity<List<ChildVaccination>> getVaccinationsForChild(@PathVariable Long childId) {
        return ResponseEntity.ok(childVaccinationRepository.findByChildIdWithDetails(childId));
    }

    @PostMapping("/{id}/administer")
    @Transactional
    public ResponseEntity<ChildVaccination> administerDose(@PathVariable Long id,
                                                          @RequestBody(required = false) Map<String, Object> body,
                                                          @RequestParam(required = false) LocalDate administeredDate,
                                                          @RequestParam(required = false) String notes) {
        User user = resolveDefaultUser();
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
