package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_documents")
@SQLRestriction("deleted_at IS NULL")
public class GeneratedDocumentEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, unique = true, length = 60)
    private String documentNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "minio_object_key", nullable = false, length = 350)
    private String minioObjectKey;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "generated_by_user_id")
    private UUID generatedByUserId;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt = OffsetDateTime.now();

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMinioObjectKey() { return minioObjectKey; }
    public void setMinioObjectKey(String minioObjectKey) { this.minioObjectKey = minioObjectKey; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public UUID getGeneratedByUserId() { return generatedByUserId; }
    public void setGeneratedByUserId(UUID generatedByUserId) { this.generatedByUserId = generatedByUserId; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
}
