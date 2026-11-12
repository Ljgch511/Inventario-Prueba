package com.globalr.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "asset_vault")
@SQLRestriction("deleted_at IS NULL")
public class AssetVaultEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false, unique = true)
    private UUID assetId;

    @Column(name = "bitlocker_recovery_key_enc", columnDefinition = "TEXT")
    private String bitlockerRecoveryKeyEnc;

    @Column(name = "device_email_enc", columnDefinition = "TEXT")
    private String deviceEmailEnc;

    @Column(name = "device_password_enc", columnDefinition = "TEXT")
    private String devicePasswordEnc;

    @Column(name = "extra_notes_enc", columnDefinition = "TEXT")
    private String extraNotesEnc;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public String getBitlockerRecoveryKeyEnc() { return bitlockerRecoveryKeyEnc; }
    public void setBitlockerRecoveryKeyEnc(String bitlockerRecoveryKeyEnc) { this.bitlockerRecoveryKeyEnc = bitlockerRecoveryKeyEnc; }
    public String getDeviceEmailEnc() { return deviceEmailEnc; }
    public void setDeviceEmailEnc(String deviceEmailEnc) { this.deviceEmailEnc = deviceEmailEnc; }
    public String getDevicePasswordEnc() { return devicePasswordEnc; }
    public void setDevicePasswordEnc(String devicePasswordEnc) { this.devicePasswordEnc = devicePasswordEnc; }
    public String getExtraNotesEnc() { return extraNotesEnc; }
    public void setExtraNotesEnc(String extraNotesEnc) { this.extraNotesEnc = extraNotesEnc; }
}
