package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findTop200ByOrderByCreatedAtDesc();
}
