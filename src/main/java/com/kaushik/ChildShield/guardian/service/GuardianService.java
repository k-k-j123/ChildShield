package com.kaushik.ChildShield.guardian.service;

import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.guardian.dto.GuardianDTO;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.user.repository.UserRepository;
import com.kaushik.ChildShield.village.model.Village;
import com.kaushik.ChildShield.village.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final VillageRepository villageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Guardian> findAllForUser(User currentUser) {
        if (currentUser.getRole() == User.Role.ADMIN) {
            return guardianRepository.findAll();
        } else if (currentUser.getRole() == User.Role.HEALTH_WORKER) {
            if (currentUser.getAssignedVillages() == null || currentUser.getAssignedVillages().isEmpty()) {
                return guardianRepository.findAll();
            }
            return guardianRepository.findByVillageIn(List.copyOf(currentUser.getAssignedVillages()));
        } else if (currentUser.getRole() == User.Role.PARENT) {
            Guardian guardian = guardianRepository.findByUserId(currentUser.getId()).orElse(null);
            return guardian != null ? List.of(guardian) : List.of();
        }
        return List.of();
    }

    public Guardian findById(Long id, User currentUser) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + id));

        validateAccess(guardian.getVillage(), currentUser);
        return guardian;
    }

    @Transactional
    public Guardian create(GuardianDTO dto, User currentUser) {
        if (dto == null || dto.getVillageId() == null) {
            throw new IllegalArgumentException("Village ID is required to register a guardian");
        }
        Village village = villageRepository.findById(dto.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with id: " + dto.getVillageId()));

        validateAccess(village, currentUser);

        User linkedUser = null;
        if (dto.getUserId() != null) {
            linkedUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
        }

        if (linkedUser == null) {
            String username = dto.getUsername();
            if (username == null || username.trim().isEmpty()) {
                String cleanPhone = dto.getPhone() != null ? dto.getPhone().replaceAll("\\D", "") : "";
                username = !cleanPhone.isEmpty() ? "mother_" + cleanPhone : ("user_" + System.currentTimeMillis());
            } else {
                username = username.trim().toLowerCase();
            }

            // Ensure unique username
            String candidateUsername = username;
            int counter = 1;
            while (userRepository.existsByUsername(candidateUsername)) {
                candidateUsername = username + counter++;
            }

            String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                    ? dto.getPassword().trim()
                    : (dto.getPhone() != null && !dto.getPhone().trim().isEmpty() ? dto.getPhone().trim() : "mother123");

            User newUser = User.builder()
                    .fullName(dto.getFullName() != null ? dto.getFullName().trim() : "Mother")
                    .username(candidateUsername)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(User.Role.PARENT)
                    .enabled(true)
                    .build();

            linkedUser = userRepository.save(newUser);
        }

        Guardian guardian = Guardian.builder()
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .village(village)
                .user(linkedUser)
                .createdBy(currentUser)
                .build();

        return guardianRepository.save(guardian);
    }

    @Transactional
    public Guardian update(Long id, GuardianDTO dto, User currentUser) {
        Guardian guardian = findById(id, currentUser);

        Village village = villageRepository.findById(dto.getVillageId())
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with id: " + dto.getVillageId()));

        validateAccess(village, currentUser);

        guardian.setFullName(dto.getFullName());
        guardian.setPhone(dto.getPhone());
        guardian.setVillage(village);

        if (dto.getUserId() != null) {
            User linkedUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
            guardian.setUser(linkedUser);
        } else {
            guardian.setUser(null);
        }

        return guardianRepository.save(guardian);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Guardian guardian = findById(id, currentUser);
        guardianRepository.delete(guardian);
    }

    public void validateAccess(Village village, User currentUser) {
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == User.Role.HEALTH_WORKER) {
            if (currentUser.getAssignedVillages() == null || currentUser.getAssignedVillages().isEmpty()) {
                return;
            }
            boolean assigned = currentUser.getAssignedVillages().stream()
                    .anyMatch(v -> v.getId().equals(village.getId()));
            if (!assigned) {
                throw new AccessDeniedException("Access denied to village: " + village.getName());
            }
            return;
        }
        throw new AccessDeniedException("Access denied");
    }
}
