package com.kaushik.ChildShield.dashboard.controller;

import com.kaushik.ChildShield.child.repository.ChildRepository;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.village.repository.VillageRepository;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final UserRepository userRepository;
    private final VillageRepository villageRepository;
    private final GuardianRepository guardianRepository;
    private final ChildRepository childRepository;
    private final ChildVaccinationRepository childVaccinationRepository;

    @GetMapping({"/api/dashboard", "/dashboard"})
    public ResponseEntity<Map<String, Object>> dashboardData() {
        return ResponseEntity.ok(Map.of(
                "totalVillages", villageRepository.count(),
                "totalWorkers", userRepository.findByRole(User.Role.HEALTH_WORKER).size(),
                "totalGuardians", guardianRepository.count(),
                "totalChildren", childRepository.count(),
                "totalDoses", childVaccinationRepository.count()
        ));
    }

    @GetMapping({"/api/reports", "/reports"})
    public ResponseEntity<Map<String, Object>> reportsData() {
        return ResponseEntity.ok(Map.of(
                "villages", villageRepository.findAll(),
                "beneficiariesCount", guardianRepository.count() + childRepository.count(),
                "completedDoses", childVaccinationRepository.findAll().stream().filter(v -> v.getStatus() == ChildVaccination.Status.COMPLETED).count()
        ));
    }

    @GetMapping({"/api/profile", "/profile"})
    public ResponseEntity<User> profileData(
            HttpSession session,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Username", required = false) String username
    ) {
        if (session != null && session.getAttribute("currentUser") instanceof User user && user.getId() != null) {
            return ResponseEntity.ok(userRepository.findById(user.getId()).orElse(user));
        }
        if (userId != null) {
            var u = userRepository.findById(userId);
            if (u.isPresent()) return ResponseEntity.ok(u.get());
        }
        if (username != null && !username.isBlank()) {
            var u = userRepository.findByUsername(username.trim());
            if (u.isPresent()) return ResponseEntity.ok(u.get());
        }
        User user = userRepository.findAll().stream().findFirst().orElse(null);
        return ResponseEntity.ok(user);
    }
}
