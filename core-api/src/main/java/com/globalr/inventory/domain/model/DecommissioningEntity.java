package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.DecommissionDisposition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "decommissionings")
@SQLRestriction("deleted_at IS NULL")
public class DecommissioningEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false, unique = true)
    private UUID assetId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecommissionDisposition disposition;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "parts_usage_notes", nullable = false, columnDefinition = "TEXT")
    private String partsUsageNotes;

    @Column(name = "recipient_info", length = 200)
    private String recipientInfo;

    @Column(name = "decommissioned_at", nullable = false)
    private OffsetDateTime decommissionedAt = OffsetDateTime.now();

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public DecommissionDisposition getDisposition() { return disposition; }
    public void setDisposition(DecommissionDisposition disposition) { this.disposition = disposition; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPartsUsageNotes() { return partsUsageNotes; }
    public void setPartsUsageNotes(String partsUsageNotes) { this.partsUsageNotes = partsUsageNotes; }
    public String getRecipientInfo() { return recipientInfo; }
    public void setRecipientInfo(String recipientInfo) { this.recipientInfo = recipientInfo; }
    public OffsetDateTime getDecommissionedAt() { return decommissionedAt; }
    public void setDecommissionedAt(OffsetDateTime decommissionedAt) { this.decommissionedAt = decommissionedAt; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(UUID approvedByUserId) { this.approvedByUserId = approvedByUserId; }
}
