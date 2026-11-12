package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.enums.DomainEnums.TicketStatus;
import com.globalr.inventory.domain.model.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {
    Optional<TicketEntity> findByIdAndCountryId(UUID id, UUID countryId);
    List<TicketEntity> findByAssetIdOrderByCreatedAtDesc(UUID assetId);
    long countByStatus(TicketStatus status);
}
