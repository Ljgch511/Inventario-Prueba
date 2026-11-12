package com.globalr.inventory.media;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import net.coobird.thumbnailator.Thumbnails;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Compresses field evidence photographs using Thumbnailator/ImageIO
 * and stores them in MinIO/S3 under country-scoped object keys.
 */
@ApplicationScoped
public class MinioMediaService {

    @ConfigProperty(name = "minio.endpoint")
    String endpoint;

    @ConfigProperty(name = "minio.access-key")
    String accessKey;

    @ConfigProperty(name = "minio.secret-key")
    String secretKey;

    @ConfigProperty(name = "minio.bucket")
    String bucket;

    private MinioClient minioClient;

    @PostConstruct
    void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public MediaDtos.MediaUploadResponse compressAndStore(Path filePath, String countryCode, String assetId, String stage) {
        long startNs = System.nanoTime();
        try {
            ensureBucket();
            byte[] originalBytes = Files.readAllBytes(filePath);
            byte[] outputBytes;
            String contentType = "image/jpeg";

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (img != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int targetW = Math.min(img.getWidth(), 1280);
                int targetH = Math.min(img.getHeight(), 1280);
                Thumbnails.of(img)
                        .size(targetW, targetH)
                        .outputFormat("jpg")
                        .outputQuality(0.78)
                        .toOutputStream(baos);
                outputBytes = baos.toByteArray();
            } else {
                outputBytes = originalBytes;
                contentType = "application/octet-stream";
            }

            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(outputBytes));
            String safeCountry = (countryCode == null || countryCode.isBlank()) ? "GLOBAL" : countryCode.toUpperCase();
            String safeAsset = (assetId == null || assetId.isBlank()) ? "unassigned" : assetId;
            String safeStage = (stage == null || stage.isBlank()) ? "GENERAL" : stage.toUpperCase();
            String objectKey = String.format("%s/evidences/%s/%s-%s.jpg", safeCountry, safeAsset, safeStage, UUID.randomUUID());

            try (ByteArrayInputStream bais = new ByteArrayInputStream(outputBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(bais, outputBytes.length, -1)
                                .contentType(contentType)
                                .build()
                );
            }

            long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);
            return new MediaDtos.MediaUploadResponse(
                    objectKey,
                    contentType,
                    originalBytes.length,
                    outputBytes.length,
                    sha256,
                    elapsedMs
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compress and upload media to MinIO", e);
        }
    }

    public byte[] fetchObject(String objectKey) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to retrieve object: " + objectKey, e);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
