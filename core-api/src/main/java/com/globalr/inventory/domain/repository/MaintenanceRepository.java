package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.MaintenanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceEntity, UUID> {
    List<MaintenanceEntity> findByAssetIdOrderByStartedAtDesc(UUID assetId);
    List<MaintenanceEntity> findAllByOrderByStartedAtDesc();
    Optional<MaintenanceEntity> findByIdAndCountryId(UUID id, UUID countryId);
}
