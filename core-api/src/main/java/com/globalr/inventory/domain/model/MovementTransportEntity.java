package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.EntrySource;
import com.globalr.inventory.domain.enums.DomainEnums.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movements_transport")
@SQLRestriction("deleted_at IS NULL")
public class MovementTransportEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_source", length = 40)
    private EntrySource entrySource;

    @Column(name = "origin_location", length = 200)
    private String originLocation;

    @Column(name = "destination_location", nullable = false, length = 200)
    private String destinationLocation;

    @Column(name = "transporter_name", length = 150)
    private String transporterName;

    @Column(name = "transporter_id_doc", length = 50)
    private String transporterIdDoc;

    @Column(name = "executed_by_user_id", nullable = false)
    private UUID executedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "moved_at", nullable = false)
    private OffsetDateTime movedAt = OffsetDateTime.now();

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public EntrySource getEntrySource() { return entrySource; }
    public void setEntrySource(EntrySource entrySource) { this.entrySource = entrySource; }
    public String getOriginLocation() { return originLocation; }
    public void setOriginLocation(String originLocation) { this.originLocation = originLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }
    public String getTransporterName() { return transporterName; }
    public void setTransporterName(String transporterName) { this.transporterName = transporterName; }
    public String getTransporterIdDoc() { return transporterIdDoc; }
    public void setTransporterIdDoc(String transporterIdDoc) { this.transporterIdDoc = transporterIdDoc; }
    public UUID getExecutedByUserId() { return executedByUserId; }
    public void setExecutedByUserId(UUID executedByUserId) { this.executedByUserId = executedByUserId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public OffsetDateTime getMovedAt() { return movedAt; }
    public void setMovedAt(OffsetDateTime movedAt) { this.movedAt = movedAt; }
}
