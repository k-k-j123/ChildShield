package com.kaushik.ChildShield.village.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VillageDTO {
    private Long id;
    private String name;
    private String district;
    private String state;
    @Builder.Default
    private boolean active = true;
}
