package com.globalr.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "os_licenses")
@SQLRestriction("deleted_at IS NULL")
public class OsLicenseEntity extends BaseTenantEntity {

    @Column(name = "os_name", nullable = false, length = 100)
    private String osName;

    @Column(nullable = false, length = 100)
    private String edition;

    @Column(name = "license_type", nullable = false, length = 30)
    private String licenseType;

    @Column(name = "license_key_masked", length = 100)
    private String licenseKeyMasked;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats = 1;

    @Column(name = "assigned_asset_id")
    private UUID assignedAssetId;

    @Column(name = "provider_name", length = 150)
    private String providerName;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "cost_usd", nullable = false, precision = 15, scale = 2)
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(name = "purchased_at")
    private LocalDate purchasedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    public String getLicenseKeyMasked() { return licenseKeyMasked; }
    public void setLicenseKeyMasked(String licenseKeyMasked) { this.licenseKeyMasked = licenseKeyMasked; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public UUID getAssignedAssetId() { return assignedAssetId; }
    public void setAssignedAssetId(UUID assignedAssetId) { this.assignedAssetId = assignedAssetId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public LocalDate getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDate purchasedAt) { this.purchasedAt = purchasedAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
