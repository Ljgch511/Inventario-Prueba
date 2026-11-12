package com.globalr.inventory.domain.repository;

import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsernameIgnoreCase(String username);
    List<UserEntity> findByCountryIdAndRoleIn(UUID countryId, List<Role> roles);
    List<UserEntity> findByCountryId(UUID countryId);
}
