package com.kaushik.ChildShield.village.repository;

import com.kaushik.ChildShield.village.model.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {
    List<Village> findByActiveTrue();
    boolean existsByNameAndDistrictAndState(String name, String district, String state);
    Optional<Village> findByNameAndDistrictAndState(String name, String district, String state);
}
