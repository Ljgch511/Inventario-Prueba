package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.TicketPriority;
import com.globalr.inventory.domain.enums.DomainEnums.TicketStatus;
import com.globalr.inventory.domain.enums.DomainEnums.TicketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@SQLRestriction("deleted_at IS NULL")
public class TicketEntity extends BaseTenantEntity {

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 40)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 40)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "target_personnel_id")
    private UUID targetPersonnelId;

    @Column(name = "assigned_maintenance_user_id")
    private UUID assignedMaintenanceUserId;

    @Column(name = "transporter_name", length = 150)
    private String transporterName;

    @Column(name = "transporter_id_doc", length = 50)
    private String transporterIdDoc;

    @Column(name = "origin_location", length = 200)
    private String originLocation;

    @Column(name = "destination_location", length = 200)
    private String destinationLocation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson = new HashMap<>();

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public TicketType getTicketType() { return ticketType; }
    public void setTicketType(TicketType ticketType) { this.ticketType = ticketType; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }
    public UUID getTargetPersonnelId() { return targetPersonnelId; }
    public void setTargetPersonnelId(UUID targetPersonnelId) { this.targetPersonnelId = targetPersonnelId; }
    public UUID getAssignedMaintenanceUserId() { return assignedMaintenanceUserId; }
    public void setAssignedMaintenanceUserId(UUID assignedMaintenanceUserId) { this.assignedMaintenanceUserId = assignedMaintenanceUserId; }
    public String getTransporterName() { return transporterName; }
    public void setTransporterName(String transporterName) { this.transporterName = transporterName; }
    public String getTransporterIdDoc() { return transporterIdDoc; }
    public void setTransporterIdDoc(String transporterIdDoc) { this.transporterIdDoc = transporterIdDoc; }
    public String getOriginLocation() { return originLocation; }
    public void setOriginLocation(String originLocation) { this.originLocation = originLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }
    public Map<String, Object> getPayloadJson() { return payloadJson; }
    public void setPayloadJson(Map<String, Object> payloadJson) { this.payloadJson = payloadJson != null ? payloadJson : new HashMap<>(); }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(UUID approvedByUserId) { this.approvedByUserId = approvedByUserId; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
