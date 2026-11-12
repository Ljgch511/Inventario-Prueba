package com.globalr.inventory.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "part_changes")
@SQLRestriction("deleted_at IS NULL")
public class PartChangeEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "maintenance_id")
    private UUID maintenanceId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "stored_part_id")
    private UUID storedPartId;

    @Column(name = "removed_part_desc", length = 255)
    private String removedPartDesc;

    @Column(name = "installed_part_desc", nullable = false, length = 255)
    private String installedPartDesc;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(UUID maintenanceId) { this.maintenanceId = maintenanceId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public UUID getStoredPartId() { return storedPartId; }
    public void setStoredPartId(UUID storedPartId) { this.storedPartId = storedPartId; }
    public String getRemovedPartDesc() { return removedPartDesc; }
    public void setRemovedPartDesc(String removedPartDesc) { this.removedPartDesc = removedPartDesc; }
    public String getInstalledPartDesc() { return installedPartDesc; }
    public void setInstalledPartDesc(String installedPartDesc) { this.installedPartDesc = installedPartDesc; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
    public UUID getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(UUID performedByUserId) { this.performedByUserId = performedByUserId; }
}
