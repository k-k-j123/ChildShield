package com.kaushik.ChildShield.vaccination.service;

import com.kaushik.ChildShield.child.model.Child;
import com.kaushik.ChildShield.vaccination.model.ChildVaccination;
import com.kaushik.ChildShield.vaccination.model.ScheduleDose;
import com.kaushik.ChildShield.vaccination.model.ScheduleVersion;
import com.kaushik.ChildShield.vaccination.model.Vaccine;
import com.kaushik.ChildShield.vaccination.repository.ChildVaccinationRepository;
import com.kaushik.ChildShield.vaccination.repository.ScheduleDoseRepository;
import com.kaushik.ChildShield.vaccination.repository.ScheduleVersionRepository;
import com.kaushik.ChildShield.vaccination.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaccinationScheduleService {

    private final VaccineRepository vaccineRepository;
    private final ScheduleVersionRepository scheduleVersionRepository;
    private final ScheduleDoseRepository scheduleDoseRepository;
    private final ChildVaccinationRepository childVaccinationRepository;

    @Transactional
    public void seedSchedule() {
        if (scheduleVersionRepository.findByStatus(ScheduleVersion.Status.ACTIVE).isPresent()) {
            return;
        }

        ScheduleVersion version = scheduleVersionRepository.save(ScheduleVersion.builder()
                .name("National Immunization Schedule (India)")
                .version("2024-V1")
                .sourceUrl("https://mohfw.gov.in")
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .status(ScheduleVersion.Status.ACTIVE)
                .build());

        // Helper to create vaccine and dose
        createVaccineAndDose(version, "BCG", "BCG", "Bacille Calmette-Guérin", 0, ScheduleDose.AgeUnit.DAYS, 1);
        createVaccineAndDose(version, "OPV0", "OPV 0", "Oral Polio Vaccine birth dose", 0, ScheduleDose.AgeUnit.DAYS, 2);
        createVaccineAndDose(version, "HEP_B1", "Hepatitis B-1", "Hepatitis B birth dose", 0, ScheduleDose.AgeUnit.DAYS, 3);

        createVaccineAndDose(version, "OPV1", "OPV 1", "Oral Polio Vaccine 1st dose", 6, ScheduleDose.AgeUnit.WEEKS, 4);
        createVaccineAndDose(version, "PENTA1", "Pentavalent 1", "Pentavalent 1 (DPT, HepB, Hib)", 6, ScheduleDose.AgeUnit.WEEKS, 5);
        createVaccineAndDose(version, "IPV1", "IPV 1", "Inactivated Polio Vaccine 1st dose", 6, ScheduleDose.AgeUnit.WEEKS, 6);
        createVaccineAndDose(version, "ROTA1", "Rotavirus 1", "Rotavirus Vaccine 1st dose", 6, ScheduleDose.AgeUnit.WEEKS, 7);

        createVaccineAndDose(version, "OPV2", "OPV 2", "Oral Polio Vaccine 2nd dose", 10, ScheduleDose.AgeUnit.WEEKS, 8);
        createVaccineAndDose(version, "PENTA2", "Pentavalent 2", "Pentavalent 2", 10, ScheduleDose.AgeUnit.WEEKS, 9);
        createVaccineAndDose(version, "ROTA2", "Rotavirus 2", "Rotavirus Vaccine 2nd dose", 10, ScheduleDose.AgeUnit.WEEKS, 10);

        createVaccineAndDose(version, "OPV3", "OPV 3", "Oral Polio Vaccine 3rd dose", 14, ScheduleDose.AgeUnit.WEEKS, 11);
        createVaccineAndDose(version, "PENTA3", "Pentavalent 3", "Pentavalent 3", 14, ScheduleDose.AgeUnit.WEEKS, 12);
        createVaccineAndDose(version, "IPV2", "IPV 2", "Inactivated Polio Vaccine 2nd dose", 14, ScheduleDose.AgeUnit.WEEKS, 13);
        createVaccineAndDose(version, "ROTA3", "Rotavirus 3", "Rotavirus Vaccine 3rd dose", 14, ScheduleDose.AgeUnit.WEEKS, 14);

        createVaccineAndDose(version, "MR1", "Measles & Rubella (MR) - 1", "MR 1st dose", 9, ScheduleDose.AgeUnit.MONTHS, 15);
        createVaccineAndDose(version, "JE1", "Japanese Encephalitis (JE) - 1", "JE 1st dose", 9, ScheduleDose.AgeUnit.MONTHS, 16);
        createVaccineAndDose(version, "VIT_A1", "Vitamin A (1st dose)", "Vitamin A 1st dose at 9 months", 9, ScheduleDose.AgeUnit.MONTHS, 17);

        createVaccineAndDose(version, "DPT_BOOSTER1", "DPT Booster-1", "DPT Booster 1st dose", 16, ScheduleDose.AgeUnit.MONTHS, 18);
        createVaccineAndDose(version, "MR2", "Measles & Rubella (MR) - 2", "MR 2nd dose", 16, ScheduleDose.AgeUnit.MONTHS, 19);
        createVaccineAndDose(version, "JE2", "Japanese Encephalitis (JE) - 2", "JE 2nd dose", 16, ScheduleDose.AgeUnit.MONTHS, 20);
    }

    private void createVaccineAndDose(ScheduleVersion version, String code, String name, String desc, int ageVal, ScheduleDose.AgeUnit unit, int seq) {
        if (scheduleDoseRepository.findByScheduleVersionIdAndDoseCode(version.getId(), code).isPresent()) {
            return;
        }

        Vaccine vaccine = vaccineRepository.findByCode(code).orElseGet(() ->
            vaccineRepository.save(Vaccine.builder().code(code).name(name).description(desc).active(true).build())
        );

        scheduleDoseRepository.save(ScheduleDose.builder()
                .scheduleVersion(version)
                .vaccine(vaccine)
                .doseCode(code)
                .displayName(name)
                .ageValue(ageVal)
                .ageUnit(unit)
                .sequenceNumber(seq)
                .active(true)
                .build());
    }

    @Transactional
    public void generateScheduleForChild(Child child) {
        if (child == null || child.getId() == null) {
            return;
        }

        ScheduleVersion activeVersion = scheduleVersionRepository.findByStatus(ScheduleVersion.Status.ACTIVE)
                .orElseGet(() -> {
                    seedSchedule();
                    return scheduleVersionRepository.findByStatus(ScheduleVersion.Status.ACTIVE)
                            .orElseThrow(() -> new IllegalStateException("No active vaccination schedule found"));
                });

        List<ScheduleDose> doses = scheduleDoseRepository.findByScheduleVersionIdAndActiveTrueOrderBySequenceNumberAsc(activeVersion.getId());

        LocalDate dob = child.getDateOfBirth() != null ? child.getDateOfBirth() : LocalDate.now();

        // Avoid duplicate vaccination entries for this child
        List<ChildVaccination> existing = childVaccinationRepository.findByChildId(child.getId());
        Set<Long> existingDoseIds = existing.stream()
                .map(v -> v.getScheduleDose().getId())
                .collect(Collectors.toSet());

        for (ScheduleDose dose : doses) {
            if (existingDoseIds.contains(dose.getId())) {
                continue;
            }

            LocalDate dueDate = calculateDueDate(dob, dose.getAgeValue(), dose.getAgeUnit());

            ChildVaccination vaccination = ChildVaccination.builder()
                    .child(child)
                    .scheduleDose(dose)
                    .dueDate(dueDate)
                    .status(determineStatus(dueDate))
                    .build();

            childVaccinationRepository.save(vaccination);
        }
    }

    private LocalDate calculateDueDate(LocalDate dob, int ageValue, ScheduleDose.AgeUnit unit) {
        return switch (unit) {
            case DAYS -> dob.plusDays(ageValue);
            case WEEKS -> dob.plusWeeks(ageValue);
            case MONTHS -> dob.plusMonths(ageValue);
            case YEARS -> dob.plusYears(ageValue);
        };
    }

    private ChildVaccination.Status determineStatus(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return ChildVaccination.Status.OVERDUE;
        } else if (dueDate.isEqual(today)) {
            return ChildVaccination.Status.DUE_TODAY;
        } else {
            return ChildVaccination.Status.UPCOMING;
        }
    }
}
