package com.kaushik.ChildShield.guardian.repository;

import com.kaushik.ChildShield.guardian.model.Guardian;
import com.kaushik.ChildShield.village.model.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    List<Guardian> findByVillageIn(List<Village> villages);
    List<Guardian> findByVillageId(Long villageId);
    Optional<Guardian> findByUserId(Long userId);
    boolean existsByPhone(String phone);
}
