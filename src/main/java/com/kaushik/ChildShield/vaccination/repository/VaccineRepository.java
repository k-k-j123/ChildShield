package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.Vaccine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaccineRepository extends JpaRepository<Vaccine, Long> {
    Optional<Vaccine> findByCode(String code);
}
