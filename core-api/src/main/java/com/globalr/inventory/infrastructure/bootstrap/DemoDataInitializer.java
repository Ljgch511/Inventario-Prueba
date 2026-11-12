package com.globalr.inventory.infrastructure.bootstrap;

import com.globalr.inventory.domain.enums.DomainEnums.*;
import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.*;
import com.globalr.inventory.domain.repository.*;
import com.globalr.inventory.infrastructure.satellites.SatelliteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds idempotent multi-country demo data (Venezuela and Colombia) on first startup
 * so all roles, QR codes, encrypted vault records, and ITSM workflows are ready to test.
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final CountryRepository countryRepo;
    private final UserRepository userRepo;
    private final PersonnelRepository personnelRepo;
    private final AssetRepository assetRepo;
    private final AssetVaultRepository vaultRepo;
    private final AssetComplementRepository complementRepo;
    private final StoredPartRepository storedPartRepo;
    private final TicketRepository ticketRepo;
    private final TicketApprovalRepository approvalRepo;
    private final AssignmentRepository assignmentRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final FinancialRecordRepository financialRepo;
    private final OsLicenseRepository licenseRepo;
    private final PasswordEncoder passwordEncoder;
    private final SatelliteClient satelliteClient;

    public DemoDataInitializer(
            CountryRepository countryRepo,
            UserRepository userRepo,
            PersonnelRepository personnelRepo,
            AssetRepository assetRepo,
            AssetVaultRepository vaultRepo,
            AssetComplementRepository complementRepo,
            StoredPartRepository storedPartRepo,
            TicketRepository ticketRepo,
            TicketApprovalRepository approvalRepo,
            AssignmentRepository assignmentRepo,
            MaintenanceRepository maintenanceRepo,
            FinancialRecordRepository financialRepo,
            OsLicenseRepository licenseRepo,
            PasswordEncoder passwordEncoder,
            SatelliteClient satelliteClient
    ) {
        this.countryRepo = countryRepo;
        this.userRepo = userRepo;
        this.personnelRepo = personnelRepo;
        this.assetRepo = assetRepo;
        this.vaultRepo = vaultRepo;
        this.complementRepo = complementRepo;
        this.storedPartRepo = storedPartRepo;
        this.ticketRepo = ticketRepo;
        this.approvalRepo = approvalRepo;
        this.assignmentRepo = assignmentRepo;
        this.maintenanceRepo = maintenanceRepo;
        this.financialRepo = financialRepo;
        this.licenseRepo = licenseRepo;
        this.passwordEncoder = passwordEncoder;
        this.satelliteClient = satelliteClient;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (countryRepo.count() > 0) {
            return;
        }
        LOG.info("Seeding GlobalR Inventory Prototype multi-country demo dataset...");

        Country ve = new Country();
        ve.setId(UUID.randomUUID());
        ve.setCode("VE");
        ve.setName("Venezuela");
        ve.setCurrencyCode("VES");
        ve = countryRepo.save(ve);

        Country co = new Country();
        co.setId(UUID.randomUUID());
        co.setCode("CO");
        co.setName("Colombia");
        co.setCurrencyCode("COP");
        co = countryRepo.save(co);

        String hash = passwordEncoder.encode("GlobalR2026!");

        UserEntity adminVe = saveUser(ve.getId(), "admin.ve", "admin.ve@globalr.local", hash, "Gabriel Canino", "V-18452100", Role.ADMIN, true);
        UserEntity supVe = saveUser(ve.getId(), "supervisor.ve", "supervisor.ve@globalr.local", hash, "Fernando Díaz", "V-16220311", Role.SUPERVISOR, false);
        UserEntity userVe = saveUser(ve.getId(), "usuario.ve", "rosa.vargas@globalr.local", hash, "Rosa Vargas", "V-20114890", Role.COMMON_USER, false);
        UserEntity maintVe = saveUser(ve.getId(), "mantenimiento.ve", "luis.hernandez@globalr.local", hash, "Luis Hernández", "V-19334512", Role.MAINTENANCE, false);
        saveUser(ve.getId(), "contabilidad.ve", "contabilidad.ve@globalr.local", hash, "Ana Contabilidad", "V-17882340", Role.ACCOUNTING, false);

        saveUser(co.getId(), "admin.co", "admin.co@globalr.local", hash, "Carlos Méndez", "CC-1020450123", Role.ADMIN, false);
        saveUser(co.getId(), "usuario.co", "usuario.co@globalr.local", hash, "Sofía Gómez", "CC-1032441980", Role.COMMON_USER, false);

        PersonnelEntity pRosa = savePerson(ve.getId(), AssigneeTargetType.PERSON, "Rosa", "Vargas", "V-20114890", null, "rosa.vargas@globalr.local", "Operaciones");
        PersonnelEntity pFernando = savePerson(ve.getId(), AssigneeTargetType.PERSON, "Fernando", "Díaz", "V-16220311", null, "supervisor.ve@globalr.local", "TI");
        PersonnelEntity pRecepcion = savePerson(ve.getId(), AssigneeTargetType.GROUP, null, null, null, "Recepción Caracas", "recepcion@globalr.local", "Recepción");
        PersonnelEntity pOficina = savePerson(ve.getId(), AssigneeTargetType.OFFICE, null, null, null, "Oficina Principal Caracas", null, "Infraestructura");
        PersonnelEntity pCarlosCo = savePerson(co.getId(), AssigneeTargetType.PERSON, "Carlos", "Méndez", "CC-1020450123", null, "admin.co@globalr.local", "TI Colombia");

        // Venezuela Assets
        AssetEntity laptop1 = saveAsset(ve.getId(), "VE", "GLR-VE-0001", "EQ-001", AssetCategory.ACTIVO, "LAPTOP",
                "Lenovo", "ThinkPad T14 Gen 4", "PF4X901A", ConditionStatus.BUENO, AssetStatus.ASIGNADO,
                "Intel Core i7-1365U", "Windows 11 Pro", "16 GB DDR5", "512 GB NVMe", true,
                "Piso 3 - Operaciones Caracas", true, "Equipo asignado a Rosa Vargas en buen estado",
                Map.of("pantalla", "14\" FHD", "cargadorUsbC", true), pRosa.getId(), 5);

        AssetEntity laptop2 = saveAsset(ve.getId(), "VE", "GLR-VE-0002", "EQ-002", AssetCategory.ACTIVO, "LAPTOP",
                "Dell", "Latitude 5440", "DL5440998Z", ConditionStatus.NUEVO, AssetStatus.DISPONIBLE,
                "Intel Core i5-1345U", "Windows 11 Pro", "16 GB DDR4", "512 GB SSD", true,
                "Almacén TI Caracas", true, "Lista para asignación",
                Map.of("pantalla", "14\" IPS"), null, 5);

        AssetEntity pcMaint = saveAsset(ve.getId(), "VE", "GLR-VE-0003", "EQ-003", AssetCategory.ACTIVO, "PC",
                "HP", "ProDesk 400 G9", "HP400G9771", ConditionStatus.REGULAR, AssetStatus.EN_MANTENIMIENTO,
                "Intel Core i5-12500", "Windows 11 Pro", "8 GB DDR4", "256 GB SSD", true,
                "Taller de Mantenimiento TI", false, "Presentó lentitud y fallo en disco SSD",
                Map.of("formato", "SFF"), pRecepcion.getId(), 3);

        AssetEntity camOficina = saveAsset(ve.getId(), "VE", "GLR-VE-0004", "CAM-01", AssetCategory.OBJETO_OFICINA, "CAMARA",
                "Hikvision", "DS-2CD2143G2", "HK99201144", ConditionStatus.BUENO, AssetStatus.ASIGNADO,
                null, null, null, null, true,
                "Lobby Recepción Caracas", false, "Cámara IP entrada principal",
                Map.of("resolucion", "4MP", "poe", true, "ubicacionExacta", "Techo Recepción"), pOficina.getId(), 5);

        AssetEntity monitorComp = saveAsset(ve.getId(), "VE", "GLR-VE-0005", "MON-01", AssetCategory.COMPLEMENTO, "MONITOR",
                "Dell", "P2422H 24\"", "CN0DELL8821", ConditionStatus.BUENO, AssetStatus.ASIGNADO,
                null, null, null, null, false,
                "Piso 3 - Operaciones Caracas", false, "Monitor secundario asociado a GLR-VE-0001",
                Map.of("pulgadas", 24, "hdmi", true, "vga", true), pRosa.getId(), 5);

        // Colombia Asset (strictly isolated to CO)
        saveAsset(co.getId(), "CO", "GLR-CO-0001", "EQ-CO-01", AssetCategory.ACTIVO, "LAPTOP",
                "Apple", "MacBook Pro 14 M3", "C02MBP3991", ConditionStatus.NUEVO, AssetStatus.ASIGNADO,
                "Apple M3 Pro", "macOS Sonoma", "18 GB Unified", "512 GB SSD", false,
                "Oficina Bogotá Zona T", false, "Equipo de gerencia Colombia",
                Map.of("color", "Space Black"), pCarlosCo.getId(), 5);

        // Complement association
        AssetComplementEntity comp = new AssetComplementEntity();
        comp.setId(UUID.randomUUID());
        comp.setCountryId(ve.getId());
        comp.setParentAssetId(laptop1.getId());
        comp.setComplementAssetId(monitorComp.getId());
        comp.setComplementType("MONITOR");
        comp.setBrand("Dell");
        comp.setModel("P2422H 24\"");
        comp.setSerialNumber("CN0DELL8821");
        comp.setNotes("Monitor asignado en estación de trabajo");
        comp.setCreatedBy("admin.ve");
        comp.setUpdatedBy("admin.ve");
        complementRepo.save(comp);

        // Encrypted Vault for laptop1
        tryEncryptVault(ve.getId(), laptop1.getId(),
                "482910-194022-883910-551029-440192-772183-102938-449102",
                "equipo.ve01@globalr.local",
                "Gl0balR#Eq01!",
                "Clave de recuperación verificada por TI");

        // Stored Parts in Warehouse (RAM / SSD)
        savePart(ve.getId(), PartType.RAM, "16 GB DDR4 3200MHz", "Kingston", "KVR32S22D8/16", "KNG-RAM-9001", "Almacén TI - Gaveta A2");
        savePart(ve.getId(), PartType.SSD, "1 TB NVMe PCIe 4.0", "Samsung", "980 PRO", "SMG-SSD-4410", "Almacén TI - Gaveta B1");

        // Assignment history for laptop1
        AssignmentEntity assign1 = new AssignmentEntity();
        assign1.setId(UUID.randomUUID());
        assign1.setCountryId(ve.getId());
        assign1.setAssetId(laptop1.getId());
        assign1.setPersonnelId(pRosa.getId());
        assign1.setAssignedAt(OffsetDateTime.now().minusDays(20));
        assign1.setReason("Dotación inicial puesto de Operaciones");
        assign1.setAssetStatusAtAssign(AssetStatus.ASIGNADO);
        assign1.setCreatedBy("supervisor.ve");
        assign1.setUpdatedBy("supervisor.ve");
        assignmentRepo.save(assign1);

        // Pending Approval Ticket (ready to approve by supervisor.ve)
        TicketEntity pendingTck = new TicketEntity();
        pendingTck.setId(UUID.randomUUID());
        pendingTck.setCountryId(ve.getId());
        pendingTck.setTicketCode("TCK-VE-DEMO01");
        pendingTck.setTicketType(TicketType.ASIGNACION);
        pendingTck.setStatus(TicketStatus.PENDIENTE_APROBACION);
        pendingTck.setPriority(TicketPriority.ALTA);
        pendingTck.setTitle("Solicitud de asignación Dell Latitude 5440 a Fernando Díaz");
        pendingTck.setReason("Ingreso a proyecto de auditoría ISO 27001");
        pendingTck.setAssetId(laptop2.getId());
        pendingTck.setRequesterUserId(userVe.getId());
        pendingTck.setTargetPersonnelId(pFernando.getId());
        pendingTck.setOriginLocation("Almacén TI Caracas");
        pendingTck.setDestinationLocation("Piso 4 - Gerencia TI");
        pendingTck.setCreatedBy("usuario.ve");
        pendingTck.setUpdatedBy("usuario.ve");
        pendingTck = ticketRepo.save(pendingTck);

        TicketApprovalEntity step = new TicketApprovalEntity();
        step.setId(UUID.randomUUID());
        step.setCountryId(ve.getId());
        step.setTicketId(pendingTck.getId());
        step.setActorUserId(userVe.getId());
        step.setAction("CREADO");
        step.setComment("Solicitud creada para aprobación de supervisor");
        approvalRepo.save(step);

        // Active Maintenance Ticket + Maintenance Order for pcMaint
        TicketEntity maintTck = new TicketEntity();
        maintTck.setId(UUID.randomUUID());
        maintTck.setCountryId(ve.getId());
        maintTck.setTicketCode("TCK-VE-MNT02");
        maintTck.setTicketType(TicketType.MANTENIMIENTO);
        maintTck.setStatus(TicketStatus.EN_PROCESO);
        maintTck.setPriority(TicketPriority.MEDIA);
        maintTck.setTitle("Revisión de almacenamiento y memoria HP ProDesk");
        maintTck.setReason("Lentitud severa reportada por Recepción");
        maintTck.setAssetId(pcMaint.getId());
        maintTck.setRequesterUserId(userVe.getId());
        maintTck.setAssignedMaintenanceUserId(maintVe.getId());
        maintTck.setApprovedByUserId(supVe.getId());
        maintTck.setApprovedAt(OffsetDateTime.now().minusDays(2));
        maintTck.setCreatedBy("usuario.ve");
        maintTck.setUpdatedBy("supervisor.ve");
        maintTck = ticketRepo.save(maintTck);

        MaintenanceEntity maint = new MaintenanceEntity();
        maint.setId(UUID.randomUUID());
        maint.setCountryId(ve.getId());
        maint.setAssetId(pcMaint.getId());
        maint.setTicketId(maintTck.getId());
        maint.setMaintenanceMode(MaintenanceMode.INTERNO);
        maint.setTechnicianUserId(maintVe.getId());
        maint.setTechnicianName(maintVe.getFullName());
        maint.setFailureDescription("Sectores dañados en SSD 256GB y saturación de RAM");
        maint.setReason("Lentitud severa reportada por Recepción");
        maint.setStatus(MaintenanceStatus.EN_REPARACION);
        maint.setStartedAt(OffsetDateTime.now().minusDays(2));
        maint.setCreatedBy("supervisor.ve");
        maint.setUpdatedBy("mantenimiento.ve");
        maintenanceRepo.save(maint);

        // Financial Record + OS License
        FinancialRecordEntity fin = new FinancialRecordEntity();
        fin.setId(UUID.randomUUID());
        fin.setCountryId(ve.getId());
        fin.setAssetId(laptop1.getId());
        fin.setAcquisitionType(AcquisitionType.COMPRA_INTERNACIONAL);
        fin.setLocalCost(new BigDecimal("52800.00"));
        fin.setLocalCurrency("VES");
        fin.setUsdReferenceCost(new BigDecimal("1320.00"));
        fin.setDepreciationRateAnnual(new BigDecimal("25.00"));
        fin.setResidualValueUsd(new BigDecimal("150.00"));
        fin.setProviderName("Lenovo Partner Latam LLC");
        fin.setInvoiceNumber("FAC-INT-2026-8812");
        fin.setWarrantyMonths(36);
        fin.setWarrantyExpiresAt(LocalDate.now().plusMonths(30));
        fin.setNotes("Garantía corporativa Premier Support 3 años");
        fin.setCreatedBy("contabilidad.ve");
        fin.setUpdatedBy("contabilidad.ve");
        financialRepo.save(fin);

        OsLicenseEntity lic = new OsLicenseEntity();
        lic.setId(UUID.randomUUID());
        lic.setCountryId(ve.getId());
        lic.setOsName("Windows 11");
        lic.setEdition("Pro 64-bit");
        lic.setLicenseType("VOLUMEN");
        lic.setLicenseKeyMasked("W11PR-XXXXX-XXXXX-9921A");
        lic.setTotalSeats(25);
        lic.setAssignedAssetId(laptop1.getId());
        lic.setProviderName("Microsoft Cloud Partner");
        lic.setInvoiceNumber("MS-LIC-2026-014");
        lic.setCostUsd(new BigDecimal("4750.00"));
        lic.setPurchasedAt(LocalDate.now().minusMonths(4));
        lic.setExpiresAt(LocalDate.now().plusYears(3));
        lic.setCreatedBy("contabilidad.ve");
        lic.setUpdatedBy("contabilidad.ve");
        licenseRepo.save(lic);

        LOG.info("GlobalR Inventory Prototype seed completed.");
    }

    private UserEntity saveUser(UUID countryId, String username, String email, String hash,
                                String fullName, String nationalId, Role role, boolean multiCountry) {
        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setCountryId(countryId);
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setFullName(fullName);
        u.setNationalId(nationalId);
        u.setRole(role);
        u.setMultiCountryAccess(multiCountry);
        u.setActive(true);
        u.setCreatedBy("bootstrap");
        u.setUpdatedBy("bootstrap");
        return userRepo.save(u);
    }

    private PersonnelEntity savePerson(UUID countryId, AssigneeTargetType type, String first, String last,
                                       String natId, String groupName, String email, String dept) {
        PersonnelEntity p = new PersonnelEntity();
        p.setId(UUID.randomUUID());
        p.setCountryId(countryId);
        p.setTargetType(type);
        p.setFirstName(first);
        p.setLastName(last);
        p.setNationalId(natId);
        p.setGroupOrOfficeName(groupName);
        p.setEmail(email);
        p.setDepartment(dept);
        p.setCreatedBy("bootstrap");
        p.setUpdatedBy("bootstrap");
        return personnelRepo.save(p);
    }

    private AssetEntity saveAsset(UUID countryId, String countryCode, String assetNumber, String eqNumber,
                                  AssetCategory cat, String subcat, String brand, String model, String serial,
                                  ConditionStatus cond, AssetStatus status, String cpu, String os, String ram, String rom,
                                  Boolean eth, String loc, boolean bitlocker, String obs, Map<String, Object> specs,
                                  UUID assigneeId, int quality) {
        AssetEntity a = new AssetEntity();
        a.setId(UUID.randomUUID());
        a.setCountryId(countryId);
        a.setAssetNumber(assetNumber);
        a.setEquipmentNumber(eqNumber);
        a.setCategory(cat);
        a.setSubcategory(subcat);
        a.setBrand(brand);
        a.setModel(model);
        a.setSerialNumber(serial);
        a.setConditionStatus(cond);
        a.setStatus(status);
        a.setProcessor(cpu);
        a.setOperatingSystem(os);
        a.setRamSpec(ram);
        a.setStorageRomSpec(rom);
        a.setHasEthernetPort(eth);
        a.setPhysicalLocation(loc);
        a.setBitlockerEnabled(bitlocker);
        a.setObservations(obs);
        a.setSpecsJson(specs);
        a.setCurrentAssigneeId(assigneeId);
        a.setQualityRating(quality);
        a.setQrTokenId("QR-" + assetNumber);
        a.setCreatedBy("bootstrap");
        a.setUpdatedBy("bootstrap");

        try {
            var res = satelliteClient.signQr(new SatelliteClient.QrSignReq(
                    a.getId().toString(), a.getAssetNumber(), a.getSerialNumber(), countryCode, a.getQrTokenId()
            ));
            if (res != null) {
                a.setQrPayloadEncrypted(res.encryptedPayload());
            }
        } catch (Exception ex) {
            LOG.debug("QR signing skipped during bootstrap for {} (will sign lazily): {}", assetNumber, ex.getMessage());
        }
        return assetRepo.save(a);
    }

    private void tryEncryptVault(UUID countryId, UUID assetId, String bitlocker, String email, String pass, String notes) {
        try {
            Map<String, String> enc = satelliteClient.encryptVaultEntries(Map.of(
                    "bitlocker", bitlocker,
                    "email", email,
                    "password", pass,
                    "notes", notes
            ));
            AssetVaultEntity v = new AssetVaultEntity();
            v.setId(UUID.randomUUID());
            v.setCountryId(countryId);
            v.setAssetId(assetId);
            v.setBitlockerRecoveryKeyEnc(enc.get("bitlocker"));
            v.setDeviceEmailEnc(enc.get("email"));
            v.setDevicePasswordEnc(enc.get("password"));
            v.setExtraNotesEnc(enc.get("notes"));
            v.setCreatedBy("bootstrap");
            v.setUpdatedBy("bootstrap");
            vaultRepo.save(v);
        } catch (Exception ex) {
            LOG.debug("Vault encryption skipped during bootstrap for {}: {}", assetId, ex.getMessage());
        }
    }

    private void savePart(UUID countryId, PartType type, String cap, String brand, String model, String serial, String loc) {
        StoredPartEntity sp = new StoredPartEntity();
        sp.setId(UUID.randomUUID());
        sp.setCountryId(countryId);
        sp.setPartType(type);
        sp.setCapacity(cap);
        sp.setBrand(brand);
        sp.setModel(model);
        sp.setSerialNumber(serial);
        sp.setStatus(StoredPartStatus.EN_ALMACEN);
        sp.setPhysicalLocation(loc);
        sp.setCreatedBy("bootstrap");
        sp.setUpdatedBy("bootstrap");
        storedPartRepo.save(sp);
    }
}
