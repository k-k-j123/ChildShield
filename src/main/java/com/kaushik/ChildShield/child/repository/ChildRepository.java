package com.kaushik.ChildShield.child.repository;

import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.village.model.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findByVillageIn(List<Village> villages);
    List<Child> findByVillageId(Long villageId);
    List<Child> findByGuardianId(Long guardianId);
}
