package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.ScheduleDose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleDoseRepository extends JpaRepository<ScheduleDose, Long> {
    List<ScheduleDose> findByScheduleVersionIdAndActiveTrueOrderBySequenceNumberAsc(Long scheduleVersionId);
    Optional<ScheduleDose> findByScheduleVersionIdAndDoseCode(Long scheduleVersionId, String doseCode);
}
