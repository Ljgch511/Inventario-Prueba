package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.AssetVaultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssetVaultRepository extends JpaRepository<AssetVaultEntity, UUID> {
    Optional<AssetVaultEntity> findByAssetIdAndCountryId(UUID assetId, UUID countryId);
}
