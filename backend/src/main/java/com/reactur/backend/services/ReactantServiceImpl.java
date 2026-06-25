package com.reactur.backend.services;

import com.reactur.backend.dtos.ReactantDtos.ReactantDetailDto;
import com.reactur.backend.dtos.ReactantDtos.ReactantDto;
import com.reactur.backend.dtos.ReactantDtos.SaveReactantDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReactantServiceImpl implements ReactantService {
    
    @Override
    public ReactantDetailDto createReactant(SaveReactantDto saveReactant) {
        return null;
    }

    @Override
    public ReactantDetailDto updateReactant(UUID uuid, SaveReactantDto saveReactant) {
        return null;
    }

    @Override
    public ReactantDetailDto getReactantDetail(UUID uuid) {
        return null;
    }

    @Override
    public List<ReactantDto> getAllReactants() {
        return List.of();
    }

    @Override
    public List<ReactantDto> getArchivedReactants() {
        return List.of();
    }

    @Override
    public void archiveReactant(UUID uuid) {

    }

    @Override
    public ReactantDto restoreReactant(UUID uuid) {
        return null;
    }
}
