package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChildVaccinationRepository extends JpaRepository<ChildVaccination, Long> {
    List<ChildVaccination> findByChildId(Long childId);
    Optional<ChildVaccination> findByChildIdAndScheduleDoseId(Long childId, Long scheduleDoseId);

    @Query("SELECT cv FROM ChildVaccination cv JOIN FETCH cv.child c JOIN FETCH cv.scheduleDose sd JOIN FETCH sd.vaccine v ORDER BY cv.dueDate ASC, cv.id ASC")
    List<ChildVaccination> findAllWithDetails();

    @Query("SELECT cv FROM ChildVaccination cv JOIN FETCH cv.child c JOIN FETCH cv.scheduleDose sd JOIN FETCH sd.vaccine v WHERE cv.child.id = :childId ORDER BY cv.dueDate ASC, cv.id ASC")
    List<ChildVaccination> findByChildIdWithDetails(@Param("childId") Long childId);

    @Query("SELECT cv FROM ChildVaccination cv JOIN FETCH cv.child c JOIN FETCH cv.scheduleDose sd JOIN FETCH sd.vaccine v WHERE c.guardian.id = :guardianId ORDER BY cv.dueDate ASC, cv.id ASC")
    List<ChildVaccination> findByGuardianIdWithDetails(@Param("guardianId") Long guardianId);

    @Query("SELECT cv FROM ChildVaccination cv JOIN FETCH cv.child c JOIN FETCH cv.scheduleDose sd JOIN FETCH sd.vaccine v WHERE c.village IN :villages ORDER BY cv.dueDate ASC, cv.id ASC")
    List<ChildVaccination> findByVillagesWithDetails(@Param("villages") List<com.kaushik.ChildShield.village.model.Village> villages);
}
