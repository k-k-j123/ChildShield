package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildVaccinationRepository extends JpaRepository<ChildVaccination, Long> {
    List<ChildVaccination> findByChildId(Long childId);
    Optional<ChildVaccination> findByChildIdAndScheduleDoseId(Long childId, Long scheduleDoseId);
}
