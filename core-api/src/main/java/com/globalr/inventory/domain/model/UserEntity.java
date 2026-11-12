package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class UserEntity extends BaseTenantEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "national_id", length = 40)
    private String nationalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "multi_country_access", nullable = false)
    private boolean multiCountryAccess = false;

    @Column(nullable = false)
    private boolean active = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isMultiCountryAccess() { return multiCountryAccess; }
    public void setMultiCountryAccess(boolean multiCountryAccess) { this.multiCountryAccess = multiCountryAccess; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
