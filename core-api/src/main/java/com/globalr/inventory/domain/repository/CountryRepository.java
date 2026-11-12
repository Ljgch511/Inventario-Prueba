package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {
    Optional<Country> findByCodeIgnoreCase(String code);
}
