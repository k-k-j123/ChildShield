package com.kaushik.ChildShield.vaccination.repository;

import com.kaushik.ChildShield.vaccination.model.ScheduleVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleVersionRepository extends JpaRepository<ScheduleVersion, Long> {
    Optional<ScheduleVersion> findByStatus(ScheduleVersion.Status status);
}
