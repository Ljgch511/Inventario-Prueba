package com.globalr.inventory.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@ApplicationScoped
public class PdfGenerationService {

    @Inject
    @Location("acta.html")
    Template actaTemplate;

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

    public PdfDtos.RenderActaResponse renderAndStore(PdfDtos.RenderActaRequest req) {
        long startNs = System.nanoTime();
        try {
            ensureBucket();
            String title = resolveTitle(req.documentType());
            String counterparty = "TRANSPORTE".equalsIgnoreCase(req.documentType())
                    ? safe(req.transporterDisplay(), "Transportista Asignado")
                    : safe(req.assigneeDisplay(), "Receptor / Técnico");

            String html = actaTemplate
                    .data("title", title)
                    .data("documentNumber", safe(req.documentNumber(), "ACT-PENDIENTE"))
                    .data("documentType", safe(req.documentType(), "ASIGNACION"))
                    .data("countryCode", safe(req.countryCode(), "VE"))
                    .data("ticketCode", safe(req.ticketCode(), "N/A"))
                    .data("issuedAt", safe(req.issuedAt(), Instant.now().toString()))
                    .data("reason", safe(req.reason(), "No especificado"))
                    .data("responsibleName", safe(req.responsibleName(), "Sistema"))
                    .data("approverName", safe(req.approverName(), "Pendiente / N/A"))
                    .data("assetNumber", safe(req.assetNumber(), "-"))
                    .data("equipmentNumber", safe(req.equipmentNumber(), "-"))
                    .data("category", safe(req.category(), "-"))
                    .data("brand", safe(req.brand(), "-"))
                    .data("model", safe(req.model(), "-"))
                    .data("serialNumber", safe(req.serialNumber(), "-"))
                    .data("conditionStatus", safe(req.conditionStatus(), "-"))
                    .data("physicalLocation", safe(req.physicalLocation(), "-"))
                    .data("assigneeDisplay", safe(req.assigneeDisplay(), "N/A"))
                    .data("originLocation", safe(req.originLocation(), "N/A"))
                    .data("destinationLocation", safe(req.destinationLocation(), "N/A"))
                    .data("transporterDisplay", safe(req.transporterDisplay(), "N/A"))
                    .data("maintenanceDisplay", safe(req.maintenanceDisplay(), "N/A"))
                    .data("notes", safe(req.notes(), "Sin observaciones adicionales"))
                    .data("counterpartySignatureLabel", counterparty)
                    .render();

            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(pdfOut);
            builder.run();

            byte[] pdfBytes = pdfOut.toByteArray();
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdfBytes));
            String safeCountry = safe(req.countryCode(), "GLOBAL").toUpperCase();
            String safeAsset = safe(req.assetId(), "general");
            String safeDocNum = safe(req.documentNumber(), "acta").replaceAll("[^A-Za-z0-9_-]", "_");
            String objectKey = String.format("%s/actas/%s/%s.pdf", safeCountry, safeAsset, safeDocNum);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(bais, pdfBytes.length, -1)
                                .contentType("application/pdf")
                                .build()
                );
            }

            long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);
            return new PdfDtos.RenderActaResponse(objectKey, sha256, pdfBytes.length, elapsedMs);
        } catch (Exception e) {
            throw new IllegalStateException("Error generating PDF acta", e);
        }
    }

    public byte[] fetchPdf(String objectKey) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch PDF from MinIO: " + objectKey, e);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String resolveTitle(String docType) {
        if (docType == null) return "Acta Oficial de Activo";
        return switch (docType.toUpperCase()) {
            case "ASIGNACION" -> "Acta Oficial de Asignación de Activo";
            case "SALIDA" -> "Acta Oficial de Salida de Activo";
            case "ENTRADA" -> "Acta Oficial de Entrada de Activo";
            case "REPARACION" -> "Acta Oficial de Reparación / Mantenimiento";
            case "TRANSPORTE" -> "Acta Oficial de Transporte y Traslado";
            default -> "Acta Oficial de Activo (" + docType + ")";
        };
    }

    private String safe(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }
}
