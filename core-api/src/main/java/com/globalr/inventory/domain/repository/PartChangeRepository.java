package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.PartChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartChangeRepository extends JpaRepository<PartChangeEntity, UUID> {
    List<PartChangeEntity> findByAssetIdOrderByChangedAtDesc(UUID assetId);
}
