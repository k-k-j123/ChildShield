package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.ScheduleDose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleDoseRepository extends JpaRepository<ScheduleDose, Long> {
    List<ScheduleDose> findByScheduleVersionIdAndActiveTrueOrderBySequenceNumberAsc(Long scheduleVersionId);
}
