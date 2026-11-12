package com.globalr.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_complements")
@SQLRestriction("deleted_at IS NULL")
public class AssetComplementEntity extends BaseTenantEntity {

    @Column(name = "parent_asset_id", nullable = false)
    private UUID parentAssetId;

    @Column(name = "complement_asset_id")
    private UUID complementAssetId;

    @Column(name = "complement_type", nullable = false, length = 60)
    private String complementType;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "serial_number", nullable = false, length = 120)
    private String serialNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

    public UUID getParentAssetId() { return parentAssetId; }
    public void setParentAssetId(UUID parentAssetId) { this.parentAssetId = parentAssetId; }
    public UUID getComplementAssetId() { return complementAssetId; }
    public void setComplementAssetId(UUID complementAssetId) { this.complementAssetId = complementAssetId; }
    public String getComplementType() { return complementType; }
    public void setComplementType(String complementType) { this.complementType = complementType; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }
    public OffsetDateTime getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(OffsetDateTime unassignedAt) { this.unassignedAt = unassignedAt; }
}
