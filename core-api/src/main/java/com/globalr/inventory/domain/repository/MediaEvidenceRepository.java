package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.MediaEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaEvidenceRepository extends JpaRepository<MediaEvidenceEntity, UUID> {
    List<MediaEvidenceEntity> findByAssetIdOrderByCapturedAtDesc(UUID assetId);
    Optional<MediaEvidenceEntity> findByIdAndCountryId(UUID id, UUID countryId);
}
