package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.AssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@SQLRestriction("deleted_at IS NULL")
public class AssignmentEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "personnel_id", nullable = false)
    private UUID personnelId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status_at_assign", nullable = false, length = 30)
    private AssetStatus assetStatusAtAssign;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status_at_return", length = 30)
    private AssetStatus assetStatusAtReturn;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getPersonnelId() { return personnelId; }
    public void setPersonnelId(UUID personnelId) { this.personnelId = personnelId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(OffsetDateTime assignedAt) { this.assignedAt = assignedAt; }
    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(OffsetDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AssetStatus getAssetStatusAtAssign() { return assetStatusAtAssign; }
    public void setAssetStatusAtAssign(AssetStatus assetStatusAtAssign) { this.assetStatusAtAssign = assetStatusAtAssign; }
    public AssetStatus getAssetStatusAtReturn() { return assetStatusAtReturn; }
    public void setAssetStatusAtReturn(AssetStatus assetStatusAtReturn) { this.assetStatusAtReturn = assetStatusAtReturn; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
