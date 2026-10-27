package com.kaushik.ChildShield.village.service;

import com.kaushik.ChildShield.village.dto.VillageDTO;
import com.kaushik.ChildShield.village.model.Village;
import com.kaushik.ChildShield.village.repository.VillageRepository;
import com.kaushik.ChildShield.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VillageService {
    private final VillageRepository villageRepository;

    public List<Village> findAllActive() {
        return villageRepository.findByActiveTrue();
    }

    public Village findById(Long id) {
        return villageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with id: " + id));
    }

    @Transactional
    public Village create(VillageDTO dto) {
        if (villageRepository.existsByNameAndDistrictAndState(dto.getName(), dto.getDistrict(), dto.getState())) {
            throw new IllegalArgumentException("Village with this name, district, and state already exists");
        }

        Village village = Village.builder()
                .name(dto.getName())
                .district(dto.getDistrict())
                .state(dto.getState())
                .active(dto.isActive())
                .build();

        return villageRepository.save(village);
    }

    @Transactional
    public Village update(Long id, VillageDTO dto) {
        Village village = findById(id);

        if (villageRepository.existsByNameAndDistrictAndState(dto.getName(), dto.getDistrict(), dto.getState())) {
            Village existing = villageRepository.findByNameAndDistrictAndState(dto.getName(), dto.getDistrict(), dto.getState()).orElse(null);
            if (existing != null && !existing.getId().equals(id)) {
                throw new IllegalArgumentException("Village with this name, district, and state already exists");
            }
        }

        village.setName(dto.getName());
        village.setDistrict(dto.getDistrict());
        village.setState(dto.getState());
        village.setActive(dto.isActive());

        return villageRepository.save(village);
    }

    @Transactional
    public void delete(Long id) {
        Village village = findById(id);
        village.setActive(false);
        villageRepository.save(village);
    }
}