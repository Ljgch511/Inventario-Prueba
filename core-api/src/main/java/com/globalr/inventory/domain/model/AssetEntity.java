package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.AssetCategory;
import com.globalr.inventory.domain.enums.DomainEnums.AssetStatus;
import com.globalr.inventory.domain.enums.DomainEnums.ConditionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "assets")
@SQLRestriction("deleted_at IS NULL")
public class AssetEntity extends BaseTenantEntity {

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "asset_number", nullable = false, length = 60)
    private String assetNumber;

    @Column(name = "equipment_number", length = 60)
    private String equipmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetCategory category;

    @Column(nullable = false, length = 60)
    private String subcategory;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 120)
    private String model;

    @Column(name = "serial_number", nullable = false, length = 120)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 30)
    private ConditionStatus conditionStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status;

    @Column(length = 120)
    private String processor;

    @Column(name = "operating_system", length = 120)
    private String operatingSystem;

    @Column(name = "ram_spec", length = 80)
    private String ramSpec;

    @Column(name = "storage_rom_spec", length = 80)
    private String storageRomSpec;

    @Column(name = "has_ethernet_port")
    private Boolean hasEthernetPort;

    @Column(name = "physical_location", nullable = false, length = 200)
    private String physicalLocation;

    @Column(name = "bitlocker_enabled", nullable = false)
    private boolean bitlockerEnabled = false;

    @Column(name = "primary_photo_key", length = 300)
    private String primaryPhotoKey;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specs_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> specsJson = new HashMap<>();

    @Column(name = "current_assignee_id")
    private UUID currentAssigneeId;

    @Column(name = "qr_token_id", length = 80)
    private String qrTokenId;

    @Column(name = "qr_payload_encrypted", columnDefinition = "TEXT")
    private String qrPayloadEncrypted;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getAssetNumber() { return assetNumber; }
    public void setAssetNumber(String assetNumber) { this.assetNumber = assetNumber; }
    public String getEquipmentNumber() { return equipmentNumber; }
    public void setEquipmentNumber(String equipmentNumber) { this.equipmentNumber = equipmentNumber; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public ConditionStatus getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(ConditionStatus conditionStatus) { this.conditionStatus = conditionStatus; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }
    public String getRamSpec() { return ramSpec; }
    public void setRamSpec(String ramSpec) { this.ramSpec = ramSpec; }
    public String getStorageRomSpec() { return storageRomSpec; }
    public void setStorageRomSpec(String storageRomSpec) { this.storageRomSpec = storageRomSpec; }
    public Boolean getHasEthernetPort() { return hasEthernetPort; }
    public void setHasEthernetPort(Boolean hasEthernetPort) { this.hasEthernetPort = hasEthernetPort; }
    public String getPhysicalLocation() { return physicalLocation; }
    public void setPhysicalLocation(String physicalLocation) { this.physicalLocation = physicalLocation; }
    public boolean isBitlockerEnabled() { return bitlockerEnabled; }
    public void setBitlockerEnabled(boolean bitlockerEnabled) { this.bitlockerEnabled = bitlockerEnabled; }
    public String getPrimaryPhotoKey() { return primaryPhotoKey; }
    public void setPrimaryPhotoKey(String primaryPhotoKey) { this.primaryPhotoKey = primaryPhotoKey; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public Map<String, Object> getSpecsJson() { return specsJson; }
    public void setSpecsJson(Map<String, Object> specsJson) { this.specsJson = specsJson != null ? specsJson : new HashMap<>(); }
    public UUID getCurrentAssigneeId() { return currentAssigneeId; }
    public void setCurrentAssigneeId(UUID currentAssigneeId) { this.currentAssigneeId = currentAssigneeId; }
    public String getQrTokenId() { return qrTokenId; }
    public void setQrTokenId(String qrTokenId) { this.qrTokenId = qrTokenId; }
    public String getQrPayloadEncrypted() { return qrPayloadEncrypted; }
    public void setQrPayloadEncrypted(String qrPayloadEncrypted) { this.qrPayloadEncrypted = qrPayloadEncrypted; }
    public Integer getQualityRating() { return qualityRating; }
    public void setQualityRating(Integer qualityRating) { this.qualityRating = qualityRating; }
}
