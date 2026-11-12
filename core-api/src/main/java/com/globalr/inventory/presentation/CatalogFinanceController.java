package com.globalr.inventory.presentation;

import com.globalr.inventory.application.CatalogAndFinanceService;
import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.model.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CatalogFinanceController {

    private final CatalogAndFinanceService service;

    public CatalogFinanceController(CatalogAndFinanceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return service.getDashboardSummary();
    }

    @GetMapping("/personnel")
    public List<PersonnelEntity> listPersonnel() {
        return service.listPersonnel();
    }

    @PostMapping("/personnel")
    public PersonnelEntity createPersonnel(@Valid @RequestBody CreatePersonnelRequest req) {
        return service.createPersonnel(req);
    }

    @GetMapping("/parts")
    public List<StoredPartEntity> listParts() {
        return service.listStoredParts();
    }

    @PostMapping("/parts")
    public StoredPartEntity createPart(@Valid @RequestBody CreateStoredPartRequest req) {
        return service.createStoredPart(req);
    }

    @GetMapping("/maintenances")
    public List<MaintenanceEntity> listMaintenances() {
        return service.listMaintenances();
    }

    @GetMapping("/movements")
    public List<MovementTransportEntity> listMovements() {
        return service.listMovements();
    }

    @GetMapping("/documents")
    public List<GeneratedDocumentEntity> listDocuments() {
        return service.listDocuments();
    }

    @GetMapping("/financial")
    public List<FinancialRecordEntity> listFinancial() {
        return service.listFinancialRecords();
    }

    @PostMapping("/financial")
    public FinancialRecordEntity createFinancial(@Valid @RequestBody CreateFinancialRequest req) {
        return service.createFinancialRecord(req);
    }

    @GetMapping("/licenses")
    public List<OsLicenseEntity> listLicenses() {
        return service.listOsLicenses();
    }

    @PostMapping("/licenses")
    public OsLicenseEntity createLicense(@Valid @RequestBody CreateOsLicenseRequest req) {
        return service.createOsLicense(req);
    }

    @GetMapping("/audit-logs")
    public List<AuditLogEntity> listAuditLogs() {
        return service.listAuditLogs();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listCountryUsers() {
        return service.listCountryUsers();
    }
}
