package com.kaushik.ChildShield.vaccination.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "schedule_doses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"schedule_version_id", "dose_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_version_id", nullable = false)
    private ScheduleVersion scheduleVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    @Column(name = "dose_code", nullable = false, length = 40)
    private String doseCode;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "age_value", nullable = false)
    private int ageValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_unit", nullable = false, length = 20)
    private AgeUnit ageUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_rule", nullable = false, length = 30)
    @Builder.Default
    private RegionRule regionRule = RegionRule.NATIONAL;

    @Column(name = "region_code", length = 40)
    private String regionCode;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum AgeUnit {
        DAYS,
        WEEKS,
        MONTHS,
        YEARS
    }

    public enum RegionRule {
        NATIONAL,
        REGION_SPECIFIC
    }
}
