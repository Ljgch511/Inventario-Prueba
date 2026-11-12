package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.enums.DomainEnums.AssetStatus;
import com.globalr.inventory.domain.model.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID>, JpaSpecificationExecutor<AssetEntity> {
    Optional<AssetEntity> findByIdAndCountryId(UUID id, UUID countryId);
    Optional<AssetEntity> findByAssetNumberAndCountryId(String assetNumber, UUID countryId);
    List<AssetEntity> findByCurrentAssigneeIdOrderByCreatedAtDesc(UUID personnelId);
    long countByStatus(AssetStatus status);
}
