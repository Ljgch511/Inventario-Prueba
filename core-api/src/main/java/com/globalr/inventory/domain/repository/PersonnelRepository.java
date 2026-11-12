package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.PersonnelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonnelRepository extends JpaRepository<PersonnelEntity, UUID> {
    List<PersonnelEntity> findAllByOrderByCreatedAtDesc();
    Optional<PersonnelEntity> findByIdAndCountryId(UUID id, UUID countryId);
    Optional<PersonnelEntity> findByNationalIdAndCountryId(String nationalId, UUID countryId);
}
