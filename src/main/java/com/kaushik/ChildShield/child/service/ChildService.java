package com.kaushik.ChildShield.child.service;

import com.kaushik.ChildShield.child.dto.ChildDTO;
import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.child.repository.ChildRepository;
import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.user.model.User;
import com.kaushik.ChildShield.village.model.Village;
import com.kaushik.ChildShield.village.repository.VillageRepository;
import com.kaushik.ChildShield.vaccination.service.VaccinationScheduleService;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import com.kaushik.ChildShield.notification.service.ReminderScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ChildVaccinationRepository childVaccinationRepository;
    private final GuardianRepository guardianRepository;
    private final VillageRepository villageRepository;
    private final GuardianService guardianService;
    private final VaccinationScheduleService vaccinationScheduleService;
    private final ReminderScheduler reminderScheduler;

    public List<Child> findAllForUser(User currentUser) {
        if (currentUser.getRole() == User.Role.ADMIN) {
            return childRepository.findAll();
        } else if (currentUser.getRole() == User.Role.HEALTH_WORKER) {
            if (currentUser.getAssignedVillages() == null || currentUser.getAssignedVillages().isEmpty()) {
                return childRepository.findAll();
            }
            return childRepository.findByVillageIn(List.copyOf(currentUser.getAssignedVillages()));
        } else if (currentUser.getRole() == User.Role.PARENT) {
            Guardian guardian = guardianRepository.findByUserId(currentUser.getId()).orElse(null);
            return guardian != null ? childRepository.findByGuardianId(guardian.getId()) : List.of();
        }
        return List.of();
    }

    public Child findById(Long id, User currentUser) {
        Child child = childRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Child not found with id: " + id));

        if (currentUser.getRole() == User.Role.PARENT) {
            Guardian guardian = guardianRepository.findByUserId(currentUser.getId()).orElse(null);
            if (guardian == null || !child.getGuardian().getId().equals(guardian.getId())) {
                throw new AccessDeniedException("Access denied to child");
            }
        } else {
            guardianService.validateAccess(child.getVillage(), currentUser);
        }

        return child;
    }

    @Transactional
    public Child create(ChildDTO dto, User currentUser) {
        if (dto == null || dto.getGuardianId() == null) {
            throw new IllegalArgumentException("Guardian is required to register a child");
        }

        Guardian guardian = guardianRepository.findById(dto.getGuardianId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + dto.getGuardianId()));

        Long resolvedVillageId = dto.getVillageId();
        if (resolvedVillageId == null && guardian.getVillage() != null) {
            resolvedVillageId = guardian.getVillage().getId();
        }
        if (resolvedVillageId == null) {
            throw new IllegalArgumentException("Village could not be resolved for Guardian #" + dto.getGuardianId());
        }

        final Long targetVillageId = resolvedVillageId;
        Village village = villageRepository.findById(targetVillageId)
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with id: " + targetVillageId));

        guardianService.validateAccess(village, currentUser);

        Child child = Child.builder()
                .guardian(guardian)
                .fullName(dto.getFullName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .birthWeightKg(dto.getBirthWeightKg())
                .village(village)
                .createdBy(currentUser)
                .build();

        Child savedChild = childRepository.save(child);

        // Generate planned vaccination schedule transactionally
        vaccinationScheduleService.generateScheduleForChild(savedChild);

        // Immediately generate overdue/due reminders for this newly added child
        try {
            reminderScheduler.sendRemindersForChild(savedChild.getId());
        } catch (Exception e) {
            log.error("Failed to generate immediate reminders for child #{}: {}", savedChild.getId(), e.getMessage());
        }

        return savedChild;
    }

    @Transactional
    public Child update(Long id, ChildDTO dto, User currentUser) {
        Child child = findById(id, currentUser);

        Guardian guardian = guardianRepository.findById(dto.getGuardianId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found with id: " + dto.getGuardianId()));

        Long resolvedUpdateVillageId = dto.getVillageId();
        if (resolvedUpdateVillageId == null && guardian.getVillage() != null) {
            resolvedUpdateVillageId = guardian.getVillage().getId();
        }
        if (resolvedUpdateVillageId == null) {
            throw new IllegalArgumentException("Village could not be resolved for Guardian #" + dto.getGuardianId());
        }
        final Long targetUpdateVillageId = resolvedUpdateVillageId;
        Village village = villageRepository.findById(targetUpdateVillageId)
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with id: " + targetUpdateVillageId));

        guardianService.validateAccess(village, currentUser);

        child.setGuardian(guardian);
        child.setFullName(dto.getFullName());
        child.setDateOfBirth(dto.getDateOfBirth());
        child.setGender(dto.getGender());
        child.setBirthWeightKg(dto.getBirthWeightKg());
        child.setVillage(village);

        return childRepository.save(child);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Child child = findById(id, currentUser);
        childVaccinationRepository.deleteAll(childVaccinationRepository.findByChildId(child.getId()));
        childRepository.delete(child);
    }
}
