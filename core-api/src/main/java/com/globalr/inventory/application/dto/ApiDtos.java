package com.globalr.inventory.application.dto;

import com.globalr.inventory.domain.enums.DomainEnums.*;
import com.globalr.inventory.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record CountryDto(UUID id, String code, String name, String currencyCode) {}

    public record UserSummaryDto(
            UUID id, String username, String email, String fullName,
            String nationalId, Role role, UUID countryId, String countryCode,
            boolean multiCountryAccess
    ) {}

    public record LoginResponse(
            String token,
            UserSummaryDto user,
            List<CountryDto> availableCountries
    ) {}

    public record CreateAssetRequest(
            @NotBlank String assetNumber,
            String equipmentNumber,
            @NotNull AssetCategory category,
            @NotBlank String subcategory,
            @NotBlank String brand,
            @NotBlank String model,
            @NotBlank String serialNumber,
            @NotNull ConditionStatus conditionStatus,
            String processor,
            String operatingSystem,
            String ramSpec,
            String storageRomSpec,
            Boolean hasEthernetPort,
            @NotBlank String physicalLocation,
            boolean bitlockerEnabled,
            String observations,
            Map<String, Object> specsJson,
            Integer qualityRating,
            String bitlockerRecoveryKey,
            String deviceEmail,
            String devicePassword
    ) {}

    public record UpdateAssetRequest(
            String equipmentNumber,
            ConditionStatus conditionStatus,
            AssetStatus status,
            String processor,
            String operatingSystem,
            String ramSpec,
            String storageRomSpec,
            Boolean hasEthernetPort,
            String physicalLocation,
            Boolean bitlockerEnabled,
            String observations,
            Map<String, Object> specsJson,
            Integer qualityRating
    ) {}

    public record AssetSummaryDto(
            UUID id, String assetNumber, String equipmentNumber, AssetCategory category,
            String subcategory, String brand, String model, String serialNumber,
            ConditionStatus conditionStatus, AssetStatus status, String physicalLocation,
            UUID currentAssigneeId, String currentAssigneeLabel, String primaryPhotoKey,
            Integer qualityRating, OffsetDateTime createdAt
    ) {}

    public record AssetDetailDto(
            UUID id, UUID countryId, Long version, String assetNumber, String equipmentNumber,
            AssetCategory category, String subcategory, String brand, String model, String serialNumber,
            ConditionStatus conditionStatus, AssetStatus status, String processor, String operatingSystem,
            String ramSpec, String storageRomSpec, Boolean hasEthernetPort, String physicalLocation,
            boolean bitlockerEnabled, String primaryPhotoKey, String observations,
            Map<String, Object> specsJson, UUID currentAssigneeId, String currentAssigneeLabel,
            String qrTokenId, boolean hasQrPayload, Integer qualityRating,
            OffsetDateTime createdAt, OffsetDateTime updatedAt,
            List<?> complements,
            List<?> assignments,
            List<?> maintenances,
            List<?> partChanges,
            List<?> tickets,
            List<?> movements,
            Object decommissioning,
            List<?> financialRecords,
            List<?> mediaEvidences,
            List<?> generatedDocuments
    ) {}

    public record AssetVaultDto(
            UUID assetId,
            String bitlockerRecoveryKey,
            String deviceEmail,
            String devicePassword,
            String extraNotes
    ) {}

    public record AddComplementRequest(
            @NotBlank String complementType,
            @NotBlank String brand,
            @NotBlank String model,
            @NotBlank String serialNumber,
            UUID complementAssetId,
            String notes
    ) {}

    public record CreatePersonnelRequest(
            @NotNull AssigneeTargetType targetType,
            String firstName,
            String lastName,
            String nationalId,
            String groupOrOfficeName,
            String email,
            String department
    ) {}

    public record CreateStoredPartRequest(
            @NotNull PartType partType,
            @NotBlank String capacity,
            @NotBlank String brand,
            String model,
            @NotBlank String serialNumber,
            String physicalLocation
    ) {}

    public record CreateTicketRequest(
            @NotNull TicketType ticketType,
            TicketPriority priority,
            @NotBlank String title,
            @NotBlank String reason,
            @NotNull UUID assetId,
            UUID targetPersonnelId,
            UUID assignedMaintenanceUserId,
            String transporterName,
            String transporterIdDoc,
            String originLocation,
            String destinationLocation,
            Map<String, Object> payloadJson
    ) {}

    public record ApproveTicketRequest(
            String comment,
            UUID assignedMaintenanceUserId,
            String transporterName,
            String transporterIdDoc,
            String destinationLocation,
            DecommissionDisposition disposition,
            String partsUsageNotes,
            String recipientInfo,
            UUID storedPartId,
            String removedPartDesc,
            String installedPartDesc,
            MaintenanceMode maintenanceMode,
            String externalProviderName,
            EntrySource entrySource
    ) {}

    public record RejectTicketRequest(@NotBlank String rejectionReason) {}

    public record CompleteMaintenanceRequest(
            @NotBlank String workPerformed,
            @NotNull AssetStatus resultingAssetStatus,
            UUID storedPartId,
            String removedPartDesc,
            String installedPartDesc,
            String partChangeReason
    ) {}

    public record GenerateActaRequest(
            @NotNull UUID assetId,
            UUID ticketId,
            @NotNull DocumentType documentType,
            @NotBlank String reason,
            String notes
    ) {}

    public record CreateFinancialRequest(
            @NotNull UUID assetId,
            @NotNull AcquisitionType acquisitionType,
            @NotNull BigDecimal localCost,
            @NotBlank String localCurrency,
            @NotNull BigDecimal usdReferenceCost,
            BigDecimal depreciationRateAnnual,
            BigDecimal residualValueUsd,
            String providerName,
            String invoiceNumber,
            Integer warrantyMonths,
            LocalDate warrantyExpiresAt,
            String leaseCounterparty,
            LocalDate leaseStartDate,
            LocalDate leaseEndDate,
            BigDecimal leaseMonthlyPrice,
            Integer consignmentInstallments,
            Integer consignmentPaidInstallments,
            Integer consignmentTermMonths,
            LocalDate consignmentDueDate,
            String notes
    ) {}

    public record CreateOsLicenseRequest(
            @NotBlank String osName,
            @NotBlank String edition,
            @NotBlank String licenseType,
            String licenseKeyMasked,
            Integer totalSeats,
            UUID assignedAssetId,
            String providerName,
            String invoiceNumber,
            BigDecimal costUsd,
            LocalDate purchasedAt,
            LocalDate expiresAt
    ) {}
}
