package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.DecommissioningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DecommissioningRepository extends JpaRepository<DecommissioningEntity, UUID> {
    Optional<DecommissioningEntity> findByAssetId(UUID assetId);
}
