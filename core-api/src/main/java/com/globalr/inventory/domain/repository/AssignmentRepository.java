package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {
    List<AssignmentEntity> findByAssetIdOrderByAssignedAtDesc(UUID assetId);
    Optional<AssignmentEntity> findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(UUID assetId);
}
