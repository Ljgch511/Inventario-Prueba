package com.globalr.inventory.media;

public class MediaDtos {
    public record MediaUploadResponse(
            String objectKey,
            String contentType,
            long originalSizeBytes,
            long compressedSizeBytes,
            String sha256Hash,
            long processingMs
    ) {}
}
