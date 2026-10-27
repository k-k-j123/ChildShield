package com.kaushik.ChildShield.maternalvisit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaternalVisitDTO {
    private Long id;
    private Long guardianId;
    private LocalDate visitDate;
    private String visitType;
    private String notes;
    private LocalDate nextVisitDate;
}
