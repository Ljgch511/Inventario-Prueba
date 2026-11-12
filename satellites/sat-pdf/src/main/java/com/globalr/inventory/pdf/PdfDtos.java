package com.globalr.inventory.pdf;

public class PdfDtos {

    public record RenderActaRequest(
            String documentNumber,
            String documentType,
            String countryCode,
            String assetId,
            String assetNumber,
            String equipmentNumber,
            String category,
            String brand,
            String model,
            String serialNumber,
            String conditionStatus,
            String physicalLocation,
            String ticketCode,
            String reason,
            String responsibleName,
            String approverName,
            String assigneeDisplay,
            String originLocation,
            String destinationLocation,
            String transporterDisplay,
            String maintenanceDisplay,
            String notes,
            String issuedAt
    ) {}

    public record RenderActaResponse(
            String objectKey,
            String sha256Hash,
            long sizeBytes,
            long processingMs
    ) {}
}
