package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.MaintenanceMode;
import com.globalr.inventory.domain.enums.DomainEnums.MaintenanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "maintenances")
@SQLRestriction("deleted_at IS NULL")
public class MaintenanceEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_mode", nullable = false, length = 20)
    private MaintenanceMode maintenanceMode;

    @Column(name = "provider_name", length = 150)
    private String providerName;

    @Column(name = "technician_user_id")
    private UUID technicianUserId;

    @Column(name = "technician_name", length = 150)
    private String technicianName;

    @Column(name = "failure_description", nullable = false, columnDefinition = "TEXT")
    private String failureDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "work_performed", columnDefinition = "TEXT")
    private String workPerformed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaintenanceStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public MaintenanceMode getMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(MaintenanceMode maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public UUID getTechnicianUserId() { return technicianUserId; }
    public void setTechnicianUserId(UUID technicianUserId) { this.technicianUserId = technicianUserId; }
    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String technicianName) { this.technicianName = technicianName; }
    public String getFailureDescription() { return failureDescription; }
    public void setFailureDescription(String failureDescription) { this.failureDescription = failureDescription; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getWorkPerformed() { return workPerformed; }
    public void setWorkPerformed(String workPerformed) { this.workPerformed = workPerformed; }
    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { this.status = status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
}
