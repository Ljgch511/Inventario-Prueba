package com.globalr.inventory.application;

import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.enums.DomainEnums.*;
import com.globalr.inventory.domain.model.*;
import com.globalr.inventory.domain.repository.*;
import com.globalr.inventory.infrastructure.audit.AuditService;
import com.globalr.inventory.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogAndFinanceService {

    private final PersonnelRepository personnelRepository;
    private final StoredPartRepository storedPartRepository;
    private final FinancialRecordRepository financialRepository;
    private final OsLicenseRepository osLicenseRepository;
    private final AssetRepository assetRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MovementTransportRepository movementRepository;
    private final TicketRepository ticketRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CatalogAndFinanceService(
            PersonnelRepository personnelRepository,
            StoredPartRepository storedPartRepository,
            FinancialRecordRepository financialRepository,
            OsLicenseRepository osLicenseRepository,
            AssetRepository assetRepository,
            MaintenanceRepository maintenanceRepository,
            MovementTransportRepository movementRepository,
            TicketRepository ticketRepository,
            GeneratedDocumentRepository documentRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.personnelRepository = personnelRepository;
        this.storedPartRepository = storedPartRepository;
        this.financialRepository = financialRepository;
        this.osLicenseRepository = osLicenseRepository;
        this.assetRepository = assetRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.movementRepository = movementRepository;
        this.ticketRepository = ticketRepository;
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        TenantContext.PrincipalInfo p = requirePrincipal();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (AssetStatus s : AssetStatus.values()) {
            byStatus.put(s.name(), assetRepository.countByStatus(s));
        }

        long pendingTickets = ticketRepository.countByStatus(TicketStatus.PENDIENTE_APROBACION);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.EN_PROCESO);
        long totalAssets = byStatus.values().stream().mapToLong(Long::longValue).sum();

        List<Map<String, Object>> brandQuality = computeBrandQualitySummary();

        return Map.of(
                "countryCode", p.countryCode(),
                "totalAssets", totalAssets,
                "assetsByStatus", byStatus,
                "pendingTickets", pendingTickets,
                "inProgressTickets", inProgressTickets,
                "brandQuality", brandQuality
        );
    }

    private List<Map<String, Object>> computeBrandQualitySummary() {
        List<AssetEntity> assets = assetRepository.findAll();
        List<MaintenanceEntity> maints = maintenanceRepository.findAll();

        Map<UUID, String> assetToBrand = new HashMap<>();
        Map<String, List<Integer>> ratingsByBrand = new HashMap<>();
        Map<String, Long> countByBrand = new HashMap<>();
        Map<String, Long> maintCountByBrand = new HashMap<>();

        for (AssetEntity a : assets) {
            assetToBrand.put(a.getId(), a.getBrand());
            countByBrand.merge(a.getBrand(), 1L, Long::sum);
            if (a.getQualityRating() != null) {
                ratingsByBrand.computeIfAbsent(a.getBrand(), k -> new ArrayList<>()).add(a.getQualityRating());
            }
        }
        for (MaintenanceEntity m : maints) {
            String brand = assetToBrand.get(m.getAssetId());
            if (brand != null) {
                maintCountByBrand.merge(brand, 1L, Long::sum);
            }
        }

        return countByBrand.entrySet().stream().map(e -> {
            String brand = e.getKey();
            List<Integer> ratings = ratingsByBrand.getOrDefault(brand, List.of());
            double avg = ratings.isEmpty() ? 5.0 : ratings.stream().mapToInt(Integer::intValue).average().orElse(5.0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("brand", brand);
            item.put("totalAssets", e.getValue());
            item.put("maintenanceIncidents", maintCountByBrand.getOrDefault(brand, 0L));
            item.put("averageQualityRating", Math.round(avg * 10.0) / 10.0);
            return item;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonnelEntity> listPersonnel() {
        requirePrincipal();
        return personnelRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public PersonnelEntity createPersonnel(CreatePersonnelRequest req) {
        TenantContext.PrincipalInfo p = requirePrincipal();
        PersonnelEntity entity = new PersonnelEntity();
        entity.setId(UUID.randomUUID());
        entity.setCountryId(p.countryId());
        entity.setTargetType(req.targetType());
        entity.setFirstName(req.firstName());
        entity.setLastName(req.lastName());
        entity.setNationalId(req.nationalId());
        entity.setGroupOrOfficeName(req.groupOrOfficeName());
        entity.setEmail(req.email());
        entity.setDepartment(req.department());
        entity.setCreatedBy(p.username());
        entity.setUpdatedBy(p.username());
        entity = personnelRepository.save(entity);
        auditService.record("CREATE", "PERSONNEL", entity.getId().toString(), Map.of("label", entity.displayLabel()));
        return entity;
    }

    @Transactional(readOnly = true)
    public List<StoredPartEntity> listStoredParts() {
        requirePrincipal();
        return storedPartRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','MAINTENANCE')")
    public StoredPartEntity createStoredPart(CreateStoredPartRequest req) {
        TenantContext.PrincipalInfo p = requirePrincipal();
        StoredPartEntity part = new StoredPartEntity();
        part.setId(UUID.randomUUID());
        part.setCountryId(p.countryId());
        part.setPartType(req.partType());
        part.setCapacity(req.capacity().trim());
        part.setBrand(req.brand().trim());
        part.setModel(req.model());
        part.setSerialNumber(req.serialNumber().trim());
        part.setStatus(StoredPartStatus.EN_ALMACEN);
        part.setPhysicalLocation(req.physicalLocation() != null ? req.physicalLocation() : "Almacén TI");
        part.setCreatedBy(p.username());
        part.setUpdatedBy(p.username());
        part = storedPartRepository.save(part);
        auditService.record("CREATE", "STORED_PART", part.getId().toString(), Map.of("serial", part.getSerialNumber()));
        return part;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceEntity> listMaintenances() {
        requirePrincipal();
        return maintenanceRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<MovementTransportEntity> listMovements() {
        requirePrincipal();
        return movementRepository.findAllByOrderByMovedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<GeneratedDocumentEntity> listDocuments() {
        requirePrincipal();
        return documentRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','ACCOUNTING')")
    public List<FinancialRecordEntity> listFinancialRecords() {
        requirePrincipal();
        return financialRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','ACCOUNTING')")
    public FinancialRecordEntity createFinancialRecord(CreateFinancialRequest req) {
        TenantContext.PrincipalInfo p = requirePrincipal();
        assetRepository.findByIdAndCountryId(req.assetId(), p.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado"));

        FinancialRecordEntity fr = new FinancialRecordEntity();
        fr.setId(UUID.randomUUID());
        fr.setCountryId(p.countryId());
        fr.setAssetId(req.assetId());
        fr.setAcquisitionType(req.acquisitionType());
        fr.setLocalCost(req.localCost());
        fr.setLocalCurrency(req.localCurrency().toUpperCase());
        fr.setUsdReferenceCost(req.usdReferenceCost());
        fr.setDepreciationRateAnnual(req.depreciationRateAnnual() != null ? req.depreciationRateAnnual() : new BigDecimal("20.00"));
        fr.setResidualValueUsd(req.residualValueUsd() != null ? req.residualValueUsd() : BigDecimal.ZERO);
        fr.setProviderName(req.providerName());
        fr.setInvoiceNumber(req.invoiceNumber());
        fr.setWarrantyMonths(req.warrantyMonths());
        fr.setWarrantyExpiresAt(req.warrantyExpiresAt());
        fr.setLeaseCounterparty(req.leaseCounterparty());
        fr.setLeaseStartDate(req.leaseStartDate());
        fr.setLeaseEndDate(req.leaseEndDate());
        fr.setLeaseMonthlyPrice(req.leaseMonthlyPrice());
        fr.setConsignmentInstallments(req.consignmentInstallments());
        fr.setConsignmentPaidInstallments(req.consignmentPaidInstallments() != null ? req.consignmentPaidInstallments() : 0);
        fr.setConsignmentTermMonths(req.consignmentTermMonths());
        fr.setConsignmentDueDate(req.consignmentDueDate());
        fr.setNotes(req.notes());
        fr.setCreatedBy(p.username());
        fr.setUpdatedBy(p.username());
        fr = financialRepository.save(fr);
        auditService.record("CREATE", "FINANCIAL_RECORD", fr.getId().toString(), Map.of(
                "acquisitionType", fr.getAcquisitionType().name(),
                "usdCost", fr.getUsdReferenceCost()
        ));
        return fr;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','ACCOUNTING')")
    public List<OsLicenseEntity> listOsLicenses() {
        requirePrincipal();
        return osLicenseRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','ACCOUNTING')")
    public OsLicenseEntity createOsLicense(CreateOsLicenseRequest req) {
        TenantContext.PrincipalInfo p = requirePrincipal();
        OsLicenseEntity lic = new OsLicenseEntity();
        lic.setId(UUID.randomUUID());
        lic.setCountryId(p.countryId());
        lic.setOsName(req.osName().trim());
        lic.setEdition(req.edition().trim());
        lic.setLicenseType(req.licenseType().trim());
        lic.setLicenseKeyMasked(req.licenseKeyMasked());
        lic.setTotalSeats(req.totalSeats() != null ? req.totalSeats() : 1);
        lic.setAssignedAssetId(req.assignedAssetId());
        lic.setProviderName(req.providerName());
        lic.setInvoiceNumber(req.invoiceNumber());
        lic.setCostUsd(req.costUsd() != null ? req.costUsd() : BigDecimal.ZERO);
        lic.setPurchasedAt(req.purchasedAt());
        lic.setExpiresAt(req.expiresAt());
        lic.setCreatedBy(p.username());
        lic.setUpdatedBy(p.username());
        lic = osLicenseRepository.save(lic);
        auditService.record("CREATE", "OS_LICENSE", lic.getId().toString(), Map.of("osName", lic.getOsName()));
        return lic;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<AuditLogEntity> listAuditLogs() {
        requirePrincipal();
        return auditLogRepository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCountryUsers() {
        TenantContext.PrincipalInfo p = requirePrincipal();
        return userRepository.findByCountryId(p.countryId()).stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("fullName", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole());
                    return m;
                }).toList();
    }

    private TenantContext.PrincipalInfo requirePrincipal() {
        TenantContext.PrincipalInfo p = TenantContext.get();
        if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida");
        return p;
    }
}
