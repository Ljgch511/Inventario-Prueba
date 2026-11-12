package com.globalr.inventory.domain.model;

import com.globalr.inventory.domain.enums.DomainEnums.MediaStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_evidences")
@SQLRestriction("deleted_at IS NULL")
public class MediaEvidenceEntity extends BaseTenantEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(name = "movement_id")
    private UUID movementId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaStage stage;

    @Column(name = "minio_object_key", nullable = false, length = 350)
    private String minioObjectKey;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "original_size_bytes", nullable = false)
    private Long originalSizeBytes;

    @Column(name = "compressed_size_bytes", nullable = false)
    private Long compressedSizeBytes;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "captured_by_user_id")
    private UUID capturedByUserId;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt = OffsetDateTime.now();

    public UUID getAssetId() { return assetId; }
    public void setAssetId(UUID assetId) { this.assetId = assetId; }
    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }
    public UUID getMovementId() { return movementId; }
    public void setMovementId(UUID movementId) { this.movementId = movementId; }
    public MediaStage getStage() { return stage; }
    public void setStage(MediaStage stage) { this.stage = stage; }
    public String getMinioObjectKey() { return minioObjectKey; }
    public void setMinioObjectKey(String minioObjectKey) { this.minioObjectKey = minioObjectKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getOriginalSizeBytes() { return originalSizeBytes; }
    public void setOriginalSizeBytes(Long originalSizeBytes) { this.originalSizeBytes = originalSizeBytes; }
    public Long getCompressedSizeBytes() { return compressedSizeBytes; }
    public void setCompressedSizeBytes(Long compressedSizeBytes) { this.compressedSizeBytes = compressedSizeBytes; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public UUID getCapturedByUserId() { return capturedByUserId; }
    public void setCapturedByUserId(UUID capturedByUserId) { this.capturedByUserId = capturedByUserId; }
    public OffsetDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(OffsetDateTime capturedAt) { this.capturedAt = capturedAt; }
}
