package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.OsLicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OsLicenseRepository extends JpaRepository<OsLicenseEntity, java.util.UUID> {
    List<OsLicenseEntity> findAllByOrderByCreatedAtDesc();
}
