package com.kaushik.ChildShield.maternalvisit.repository;

import com.kaushik.ChildShield.maternalvisit.model.MaternalVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaternalVisitRepository extends JpaRepository<MaternalVisit, Long> {
    List<MaternalVisit> findByGuardianId(Long guardianId);
}
