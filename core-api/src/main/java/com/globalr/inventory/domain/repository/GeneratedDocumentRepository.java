package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.GeneratedDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocumentEntity, UUID> {
    List<GeneratedDocumentEntity> findByAssetIdOrderByGeneratedAtDesc(UUID assetId);
    List<GeneratedDocumentEntity> findAllByOrderByGeneratedAtDesc();
    Optional<GeneratedDocumentEntity> findByIdAndCountryId(UUID id, UUID countryId);
}
