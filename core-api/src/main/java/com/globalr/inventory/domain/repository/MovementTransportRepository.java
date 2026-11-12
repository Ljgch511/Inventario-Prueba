package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.MovementTransportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovementTransportRepository extends JpaRepository<MovementTransportEntity, UUID> {
    List<MovementTransportEntity> findByAssetIdOrderByMovedAtDesc(UUID assetId);
    List<MovementTransportEntity> findAllByOrderByMovedAtDesc();
}
