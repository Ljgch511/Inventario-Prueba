package com.globalr.inventory.infrastructure.satellites;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP Client orchestrating stateless Quarkus satellite microservices
 * (Crypto/QR, PDF Actas, Media Compression, Async Mailer).
 */
@Component
public class SatelliteClient {

    private static final Logger LOG = LoggerFactory.getLogger(SatelliteClient.class);

    private final RestClient cryptoClient;
    private final RestClient pdfClient;
    private final RestClient mediaClient;
    private final RestClient notifyClient;

    public SatelliteClient(
            @Value("${app.satellites.crypto-url}") String cryptoUrl,
            @Value("${app.satellites.pdf-url}") String pdfUrl,
            @Value("${app.satellites.media-url}") String mediaUrl,
            @Value("${app.satellites.notify-url}") String notifyUrl
    ) {
        this.cryptoClient = RestClient.builder().baseUrl(cryptoUrl).build();
        this.pdfClient = RestClient.builder().baseUrl(pdfUrl).build();
        this.mediaClient = RestClient.builder().baseUrl(mediaUrl).build();
        this.notifyClient = RestClient.builder().baseUrl(notifyUrl).build();
    }

    public record QrSignReq(String assetId, String assetNumber, String serialNumber, String countryCode, String qrTokenId) {}
    public record QrSignRes(String encryptedPayload, String qrPngBase64, long processingMs) {}
    public record QrVerifyReq(String encryptedPayload) {}
    public record QrVerifyRes(boolean valid, String assetId, String assetNumber, String serialNumber,
                              String countryCode, String qrTokenId, String issuedAt, long processingMs, String error) {}
    public record QrRenderRes(String qrPngBase64) {}

    public record VaultMapReq(Map<String, String> entries) {}
    public record VaultEncRes(Map<String, String> encryptedEntries) {}
    public record VaultDecReq(Map<String, String> encryptedEntries) {}
    public record VaultDecRes(Map<String, String> entries) {}

    public record MediaUploadRes(String objectKey, String contentType, long originalSizeBytes,
                                 long compressedSizeBytes, String sha256Hash, long processingMs) {}

    public record RenderActaReq(
            String documentNumber, String documentType, String countryCode,
            String assetId, String assetNumber, String equipmentNumber,
            String category, String brand, String model, String serialNumber,
            String conditionStatus, String physicalLocation, String ticketCode,
            String reason, String responsibleName, String approverName,
            String assigneeDisplay, String originLocation, String destinationLocation,
            String transporterDisplay, String maintenanceDisplay, String notes, String issuedAt
    ) {}
    public record RenderActaRes(String objectKey, String sha256Hash, long sizeBytes, long processingMs) {}

    public record NotifyTicketReq(
            String ticketCode, String ticketType, String eventStatus, String countryCode,
            String assetNumber, String assetSummary, String reason, String actorName, List<String> recipients
    ) {}

    public QrSignRes signQr(QrSignReq req) {
        return cryptoClient.post().uri("/api/v1/crypto/qr/sign")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req).retrieve().body(QrSignRes.class);
    }

    public QrVerifyRes verifyQr(String encryptedPayload) {
        return cryptoClient.post().uri("/api/v1/crypto/qr/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new QrVerifyReq(encryptedPayload)).retrieve().body(QrVerifyRes.class);
    }

    public String renderQrPngBase64(String encryptedPayload) {
        QrRenderRes res = cryptoClient.post().uri("/api/v1/crypto/qr/render")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new QrVerifyReq(encryptedPayload)).retrieve().body(QrRenderRes.class);
        return res != null ? res.qrPngBase64() : null;
    }

    public Map<String, String> encryptVaultEntries(Map<String, String> plain) {
        VaultEncRes res = cryptoClient.post().uri("/api/v1/crypto/vault/encrypt")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VaultMapReq(plain)).retrieve().body(VaultEncRes.class);
        return res != null ? res.encryptedEntries() : Map.of();
    }

    public Map<String, String> decryptVaultEntries(Map<String, String> encrypted) {
        VaultDecRes res = cryptoClient.post().uri("/api/v1/crypto/vault/decrypt")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VaultDecReq(encrypted)).retrieve().body(VaultDecRes.class);
        return res != null ? res.entries() : Map.of();
    }

    public MediaUploadRes uploadAndCompressPhoto(byte[] fileBytes, String filename, String countryCode, String assetId, String stage) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "photo.jpg";
            }
        };
        body.add("file", resource);
        body.add("countryCode", countryCode);
        body.add("assetId", assetId);
        body.add("stage", stage);

        return mediaClient.post().uri("/api/v1/media/compress-and-store")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body).retrieve().body(MediaUploadRes.class);
    }

    public byte[] fetchMediaBytes(String objectKey) {
        return mediaClient.get().uri(uriBuilder -> uriBuilder.path("/api/v1/media/object").queryParam("key", objectKey).build())
                .retrieve().body(byte[].class);
    }

    public RenderActaRes renderActaPdf(RenderActaReq req) {
        return pdfClient.post().uri("/api/v1/pdf/actas/render")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req).retrieve().body(RenderActaRes.class);
    }

    public byte[] fetchPdfBytes(String objectKey) {
        return pdfClient.get().uri(uriBuilder -> uriBuilder.path("/api/v1/pdf/download").queryParam("key", objectKey).build())
                .retrieve().body(byte[].class);
    }

    public void notifyTicketEventAsync(NotifyTicketReq req) {
        if (req.recipients() == null || req.recipients().isEmpty()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                notifyClient.post().uri("/api/v1/notify/ticket-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(req).retrieve().toBodilessEntity();
            } catch (Exception ex) {
                LOG.warn("Async notification dispatch failed for ticket {}: {}", req.ticketCode(), ex.getMessage());
            }
        });
    }
}
