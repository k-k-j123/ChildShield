package com.kaushik.ChildShield.child.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDTO {
    private Long id;
    private Long guardianId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private BigDecimal birthWeightKg;
    private Long villageId;
}
