package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.PartType;
import com.globalr.inventory.domain.enums.DomainEnums.StoredPartStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "stored_parts")
@SQLRestriction("deleted_at IS NULL")
public class StoredPartEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", nullable = false, length = 40)
    private PartType partType;

    @Column(nullable = false, length = 60)
    private String capacity;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(length = 120)
    private String model;

    @Column(name = "serial_number", nullable = false, length = 120)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StoredPartStatus status;

    @Column(name = "installed_in_asset_id")
    private UUID installedInAssetId;

    @Column(name = "physical_location", length = 200)
    private String physicalLocation;

    public PartType getPartType() { return partType; }
    public void setPartType(PartType partType) { this.partType = partType; }
    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public StoredPartStatus getStatus() { return status; }
    public void setStatus(StoredPartStatus status) { this.status = status; }
    public UUID getInstalledInAssetId() { return installedInAssetId; }
    public void setInstalledInAssetId(UUID installedInAssetId) { this.installedInAssetId = installedInAssetId; }
    public String getPhysicalLocation() { return physicalLocation; }
    public void setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; }
}
