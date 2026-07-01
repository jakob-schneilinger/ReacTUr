package com.reactur.backend.services.implementations;

import com.reactur.backend.dtos.ReactantDtos.StructureDto;
import com.reactur.backend.services.StructureService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class StructureServiceImpl implements StructureService {
    @Override
    public StructureDto uploadStructure(UUID reactantId, MultipartFile file) {
        return null;
    }

    @Override
    public void generateStructureFromAPI(UUID reactantId) {

    }

    @Override
    public StructureDto selectStructure(UUID structureId) {
        return null;
    }

    @Override
    public List<StructureDto> getStructuresByReactant(UUID reactantId) {
        return List.of();
    }

    @Override
    public void fetchPendingStructuresFromAPI() {

    }
}
