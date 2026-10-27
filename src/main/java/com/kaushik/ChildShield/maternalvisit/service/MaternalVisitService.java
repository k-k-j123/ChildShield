package com.kaushik.ChildShield.maternalvisit.service;

import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.guardian.repository.GuardianRepository;
import com.kaushik.ChildShield.guardian.service.GuardianService;
import com.kaushik.ChildShield.maternalvisit.dto.MaternalVisitDTO;
import com.kaushik.ChildShield.maternalvisit.model.MaternalVisit;
import com.kaushik.ChildShield.maternalvisit.repository.MaternalVisitRepository;
import com.kaushik.ChildShield.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaternalVisitService {

    private final MaternalVisitRepository maternalVisitRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianService guardianService;

    public List<MaternalVisit> findByGuardian(Long guardianId, User currentUser) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));
        guardianService.validateAccess(guardian.getVillage(), currentUser);
        return maternalVisitRepository.findByGuardianId(guardianId);
    }

    @Transactional
    public MaternalVisit create(MaternalVisitDTO dto, User currentUser) {
        Guardian guardian = guardianRepository.findById(dto.getGuardianId())
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found"));
        guardianService.validateAccess(guardian.getVillage(), currentUser);

        MaternalVisit visit = MaternalVisit.builder()
                .guardian(guardian)
                .visitDate(dto.getVisitDate())
                .visitType(dto.getVisitType())
                .notes(dto.getNotes())
                .nextVisitDate(dto.getNextVisitDate())
                .recordedBy(currentUser)
                .build();

        return maternalVisitRepository.save(visit);
    }
}
