package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.StoredPartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredPartRepository extends JpaRepository<StoredPartEntity, UUID> {
    List<StoredPartEntity> findAllByOrderByCreatedAtDesc();
    Optional<StoredPartEntity> findByIdAndCountryId(UUID id, UUID countryId);
}
