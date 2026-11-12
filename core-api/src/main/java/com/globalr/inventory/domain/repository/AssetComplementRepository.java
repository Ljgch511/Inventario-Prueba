package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.AssetComplementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetComplementRepository extends JpaRepository<AssetComplementEntity, UUID> {
    List<AssetComplementEntity> findByParentAssetIdOrderByAssignedAtDesc(UUID parentAssetId);
}
