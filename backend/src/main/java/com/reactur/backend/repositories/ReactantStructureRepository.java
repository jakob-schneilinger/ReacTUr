package com.reactur.backend.repositories;

import com.reactur.backend.entities.ReactantStructure;
import com.reactur.backend.enums.StructureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReactantStructureRepository extends JpaRepository<ReactantStructure, UUID> {

    List<ReactantStructure> findByReactant_Id(UUID id);

    List<ReactantStructure> findByFlag(StructureFlag flag);
}
