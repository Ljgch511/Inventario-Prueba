package com.globalr.inventory.application;

import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.enums.DomainEnums.*;
import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.*;
import com.globalr.inventory.domain.repository.*;
import com.globalr.inventory.infrastructure.audit.AuditService;
import com.globalr.inventory.infrastructure.satellites.SatelliteClient;
import com.globalr.inventory.infrastructure.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetVaultRepository vaultRepository;
    private final AssetComplementRepository complementRepository;
    private final PersonnelRepository personnelRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PartChangeRepository partChangeRepository;
    private final TicketRepository ticketRepository;
    private final MovementTransportRepository movementRepository;
    private final DecommissioningRepository decommissioningRepository;
    private final FinancialRecordRepository financialRepository;
    private final MediaEvidenceRepository mediaRepository;
    private final GeneratedDocumentRepository documentRepository;
    private final SatelliteClient satelliteClient;
    private final AuditService auditService;

    public AssetService(
            AssetRepository assetRepository,
            AssetVaultRepository vaultRepository,
            AssetComplementRepository complementRepository,
            PersonnelRepository personnelRepository,
            UserRepository userRepository,
            AssignmentRepository assignmentRepository,
            MaintenanceRepository maintenanceRepository,
            PartChangeRepository partChangeRepository,
            TicketRepository ticketRepository,
            MovementTransportRepository movementRepository,
            DecommissioningRepository decommissioningRepository,
            FinancialRecordRepository financialRepository,
            MediaEvidenceRepository mediaRepository,
            GeneratedDocumentRepository documentRepository,
            SatelliteClient satelliteClient,
            AuditService auditService
    ) {
        this.assetRepository = assetRepository;
        this.vaultRepository = vaultRepository;
        this.complementRepository = complementRepository;
        this.personnelRepository = personnelRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.partChangeRepository = partChangeRepository;
        this.ticketRepository = ticketRepository;
        this.movementRepository = movementRepository;
        this.decommissioningRepository = decommissioningRepository;
        this.financialRepository = financialRepository;
        this.mediaRepository = mediaRepository;
        this.documentRepository = documentRepository;
        this.satelliteClient = satelliteClient;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AssetSummaryDto> listAssets(AssetCategory category, AssetStatus status, String search) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        List<AssetEntity> assets;

        if (principal.role() == Role.COMMON_USER) {
            UUID myPersonnelId = resolvePersonnelIdForUser(principal);
            if (myPersonnelId == null) {
                return List.of();
            }
            assets = assetRepository.findByCurrentAssigneeIdOrderByCreatedAtDesc(myPersonnelId).stream()
                    .filter(a -> category == null || a.getCategory() == category)
                    .filter(a -> status == null || a.getStatus() == status)
                    .filter(a -> matchesSearch(a, search))
                    .toList();
        } else {
            String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
            Specification<AssetEntity> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (category != null) {
                    predicates.add(cb.equal(root.get("category"), category));
                }
                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                if (normalizedSearch != null) {
                    String pattern = "%" + normalizedSearch + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("assetNumber")), pattern),
                            cb.like(cb.lower(root.get("serialNumber")), pattern),
                            cb.like(cb.lower(root.get("brand")), pattern),
                            cb.like(cb.lower(root.get("model")), pattern)
                    ));
                }
                return cb.and(predicates.toArray(Predicate[]::new));
            };
            assets = assetRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Map<UUID, String> personnelLabels = loadPersonnelLabelMap();
        return assets.stream()
                .map(a -> toSummary(a, personnelLabels.get(a.getCurrentAssigneeId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssetDetailDto getAssetDetail(UUID assetId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = requireTenantAsset(assetId);

        boolean isCommonUser = principal.role() == Role.COMMON_USER;
        if (isCommonUser) {
            UUID myPersonnelId = resolvePersonnelIdForUser(principal);
            if (myPersonnelId == null || !myPersonnelId.equals(asset.getCurrentAssigneeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede consultar activos asignados a su persona");
            }
        }

        String assigneeLabel = null;
        if (asset.getCurrentAssigneeId() != null) {
            assigneeLabel = personnelRepository.findByIdAndCountryId(asset.getCurrentAssigneeId(), principal.countryId())
                    .map(PersonnelEntity::displayLabel)
                    .orElse(null);
        }

        return new AssetDetailDto(
                asset.getId(),
                asset.getCountryId(),
                asset.getVersion(),
                asset.getAssetNumber(),
                asset.getEquipmentNumber(),
                asset.getCategory(),
                asset.getSubcategory(),
                asset.getBrand(),
                asset.getModel(),
                asset.getSerialNumber(),
                asset.getConditionStatus(),
                asset.getStatus(),
                asset.getProcessor(),
                asset.getOperatingSystem(),
                asset.getRamSpec(),
                asset.getStorageRomSpec(),
                asset.getHasEthernetPort(),
                asset.getPhysicalLocation(),
                asset.isBitlockerEnabled(),
                asset.getPrimaryPhotoKey(),
                isCommonUser ? null : asset.getObservations(),
                asset.getSpecsJson(),
                asset.getCurrentAssigneeId(),
                assigneeLabel,
                asset.getQrTokenId(),
                asset.getQrPayloadEncrypted() != null,
                asset.getQualityRating(),
                asset.getCreatedAt(),
                asset.getUpdatedAt(),
                complementRepository.findByParentAssetIdOrderByAssignedAtDesc(assetId),
                isCommonUser ? List.of() : assignmentRepository.findByAssetIdOrderByAssignedAtDesc(assetId),
                isCommonUser ? List.of() : maintenanceRepository.findByAssetIdOrderByStartedAtDesc(assetId),
                isCommonUser ? List.of() : partChangeRepository.findByAssetIdOrderByChangedAtDesc(assetId),
                ticketRepository.findByAssetIdOrderByCreatedAtDesc(assetId),
                isCommonUser ? List.of() : movementRepository.findByAssetIdOrderByMovedAtDesc(assetId),
                isCommonUser ? null : decommissioningRepository.findByAssetId(assetId).orElse(null),
                isCommonUser ? List.of() : financialRepository.findByAssetIdOrderByCreatedAtDesc(assetId),
                mediaRepository.findByAssetIdOrderByCapturedAtDesc(assetId),
                isCommonUser ? List.of() : documentRepository.findByAssetIdOrderByGeneratedAtDesc(assetId)
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public AssetDetailDto createAsset(CreateAssetRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        assetRepository.findByAssetNumberAndCountryId(req.assetNumber().trim(), principal.countryId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un activo con el número " + req.assetNumber());
                });

        AssetEntity asset = new AssetEntity();
        asset.setId(UUID.randomUUID());
        asset.setCountryId(principal.countryId());
        asset.setAssetNumber(req.assetNumber().trim());
        asset.setEquipmentNumber(req.equipmentNumber());
        asset.setCategory(req.category());
        asset.setSubcategory(req.subcategory().trim());
        asset.setBrand(req.brand().trim());
        asset.setModel(req.model().trim());
        asset.setSerialNumber(req.serialNumber().trim());
        asset.setConditionStatus(req.conditionStatus());
        asset.setStatus(AssetStatus.DISPONIBLE);
        asset.setProcessor(req.processor());
        asset.setOperatingSystem(req.operatingSystem());
        asset.setRamSpec(req.ramSpec());
        asset.setStorageRomSpec(req.storageRomSpec());
        asset.setHasEthernetPort(req.hasEthernetPort());
        asset.setPhysicalLocation(req.physicalLocation().trim());
        asset.setBitlockerEnabled(req.bitlockerEnabled());
        asset.setObservations(req.observations());
        asset.setSpecsJson(req.specsJson() != null ? req.specsJson() : new HashMap<>());
        asset.setQualityRating(req.qualityRating() != null ? req.qualityRating() : 5);
        asset.setQrTokenId("QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        asset.setCreatedBy(principal.username());
        asset.setUpdatedBy(principal.username());

        try {
            var signed = satelliteClient.signQr(new SatelliteClient.QrSignReq(
                    asset.getId().toString(), asset.getAssetNumber(), asset.getSerialNumber(),
                    principal.countryCode(), asset.getQrTokenId()
            ));
            if (signed != null) {
                asset.setQrPayloadEncrypted(signed.encryptedPayload());
            }
        } catch (Exception ignored) {
            // Lazy signing fallback supported in getAssetQr
        }

        asset = assetRepository.save(asset);

        if (hasAnySecret(req.bitlockerRecoveryKey(), req.deviceEmail(), req.devicePassword())) {
            saveEncryptedVault(asset.getId(), principal, req.bitlockerRecoveryKey(), req.deviceEmail(), req.devicePassword(), null);
        }

        auditService.record("CREATE", "ASSET", asset.getId().toString(), Map.of(
                "assetNumber", asset.getAssetNumber(),
                "category", asset.getCategory().name(),
                "serialNumber", asset.getSerialNumber()
        ));

        return getAssetDetail(asset.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public AssetDetailDto updateAsset(UUID assetId, UpdateAssetRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = requireTenantAsset(assetId);

        if (req.equipmentNumber() != null) asset.setEquipmentNumber(req.equipmentNumber());
        if (req.conditionStatus() != null) asset.setConditionStatus(req.conditionStatus());
        if (req.status() != null) asset.setStatus(req.status());
        if (req.processor() != null) asset.setProcessor(req.processor());
        if (req.operatingSystem() != null) asset.setOperatingSystem(req.operatingSystem());
        if (req.ramSpec() != null) asset.setRamSpec(req.ramSpec());
        if (req.storageRomSpec() != null) asset.setStorageRomSpec(req.storageRomSpec());
        if (req.hasEthernetPort() != null) asset.setHasEthernetPort(req.hasEthernetPort());
        if (req.physicalLocation() != null) asset.setPhysicalLocation(req.physicalLocation());
        if (req.bitlockerEnabled() != null) asset.setBitlockerEnabled(req.bitlockerEnabled());
        if (req.observations() != null) asset.setObservations(req.observations());
        if (req.specsJson() != null) asset.setSpecsJson(req.specsJson());
        if (req.qualityRating() != null) asset.setQualityRating(req.qualityRating());
        asset.setUpdatedBy(principal.username());

        assetRepository.save(asset);
        auditService.record("UPDATE", "ASSET", assetId.toString(), Map.of("assetNumber", asset.getAssetNumber()));
        return getAssetDetail(assetId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void softDeleteAsset(UUID assetId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = requireTenantAsset(assetId);
        asset.markDeleted(principal.username());
        assetRepository.save(asset);
        auditService.record("SOFT_DELETE", "ASSET", assetId.toString(), Map.of("assetNumber", asset.getAssetNumber()));
    }

    @Transactional
    public Map<String, Object> getAssetQr(UUID assetId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = requireTenantAsset(assetId);

        String pngBase64;
        long processingMs = 0L;
        if (asset.getQrPayloadEncrypted() == null || asset.getQrPayloadEncrypted().isBlank()) {
            if (asset.getQrTokenId() == null) {
                asset.setQrTokenId("QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }
            var signed = satelliteClient.signQr(new SatelliteClient.QrSignReq(
                    asset.getId().toString(), asset.getAssetNumber(), asset.getSerialNumber(),
                    principal.countryCode(), asset.getQrTokenId()
            ));
            asset.setQrPayloadEncrypted(signed.encryptedPayload());
            assetRepository.save(asset);
            pngBase64 = signed.qrPngBase64();
            processingMs = signed.processingMs();
        } else {
            pngBase64 = satelliteClient.renderQrPngBase64(asset.getQrPayloadEncrypted());
        }

        return Map.of(
                "assetId", asset.getId(),
                "assetNumber", asset.getAssetNumber(),
                "qrTokenId", asset.getQrTokenId(),
                "encryptedPayload", asset.getQrPayloadEncrypted(),
                "qrPngBase64", pngBase64 != null ? pngBase64 : "",
                "processingMs", processingMs
        );
    }

    @Transactional
    public Map<String, Object> scanAndVerifyQr(String encryptedPayload) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        var verified = satelliteClient.verifyQr(encryptedPayload);
        if (verified == null || !verified.valid()) {
            auditService.record("QR_SCAN_INVALID", "ASSET", "unknown", Map.of(
                    "error", verified != null && verified.error() != null ? verified.error() : "Inválido"
            ));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    verified != null ? verified.error() : "Código QR cifrado inválido");
        }

        UUID assetId = UUID.fromString(verified.assetId());
        AssetEntity asset = assetRepository.findByIdAndCountryId(assetId, principal.countryId())
                .orElseThrow(() -> {
                    auditService.record("QR_SCAN_CROSS_TENANT_BLOCKED", "ASSET", verified.assetId(), Map.of(
                            "tokenCountry", verified.countryCode(),
                            "userCountry", principal.countryCode()
                    ));
                    return new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "El activo escaneado pertenece a otro país (" + verified.countryCode() + ") o no existe");
                });

        auditService.record("QR_SCAN", "ASSET", asset.getId().toString(), Map.of(
                "assetNumber", asset.getAssetNumber(),
                "processingMs", verified.processingMs()
        ));

        String assigneeLabel = asset.getCurrentAssigneeId() != null
                ? personnelRepository.findByIdAndCountryId(asset.getCurrentAssigneeId(), principal.countryId())
                    .map(PersonnelEntity::displayLabel).orElse(null)
                : null;

        return Map.of(
                "valid", true,
                "processingMs", verified.processingMs(),
                "issuedAt", verified.issuedAt(),
                "asset", toSummary(asset, assigneeLabel)
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public AssetComplementEntity addComplement(UUID parentAssetId, AddComplementRequest req) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        requireTenantAsset(parentAssetId);

        AssetComplementEntity comp = new AssetComplementEntity();
        comp.setId(UUID.randomUUID());
        comp.setCountryId(principal.countryId());
        comp.setParentAssetId(parentAssetId);
        comp.setComplementAssetId(req.complementAssetId());
        comp.setComplementType(req.complementType().trim());
        comp.setBrand(req.brand().trim());
        comp.setModel(req.model().trim());
        comp.setSerialNumber(req.serialNumber().trim());
        comp.setNotes(req.notes());
        comp.setCreatedBy(principal.username());
        comp.setUpdatedBy(principal.username());
        comp = complementRepository.save(comp);

        auditService.record("COMPLEMENT_ADD", "ASSET", parentAssetId.toString(), Map.of(
                "complementType", comp.getComplementType(),
                "serialNumber", comp.getSerialNumber()
        ));
        return comp;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public AssetVaultDto getVault(UUID assetId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        requireTenantAsset(assetId);
        AssetVaultEntity vault = vaultRepository.findByAssetIdAndCountryId(assetId, principal.countryId())
                .orElse(null);

        auditService.record("VAULT_READ", "ASSET_VAULT", assetId.toString(), Map.of("hasVaultRecord", vault != null));
        if (vault == null) {
            return new AssetVaultDto(assetId, null, null, null, null);
        }

        Map<String, String> encMap = new HashMap<>();
        encMap.put("bitlocker", vault.getBitlockerRecoveryKeyEnc());
        encMap.put("email", vault.getDeviceEmailEnc());
        encMap.put("password", vault.getDevicePasswordEnc());
        encMap.put("notes", vault.getExtraNotesEnc());
        Map<String, String> plain = satelliteClient.decryptVaultEntries(encMap);

        return new AssetVaultDto(
                assetId,
                plain.get("bitlocker"),
                plain.get("email"),
                plain.get("password"),
                plain.get("notes")
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public AssetVaultDto upsertVault(UUID assetId, AssetVaultDto dto) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        requireTenantAsset(assetId);
        saveEncryptedVault(assetId, principal, dto.bitlockerRecoveryKey(), dto.deviceEmail(), dto.devicePassword(), dto.extraNotes());
        auditService.record("VAULT_UPDATE", "ASSET_VAULT", assetId.toString(), Map.of("updated", true));
        return getVault(assetId);
    }

    @Transactional
    public MediaEvidenceEntity uploadEvidence(UUID assetId, UUID ticketId, MediaStage stage, MultipartFile file) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        AssetEntity asset = requireTenantAsset(assetId);
        try {
            var uploaded = satelliteClient.uploadAndCompressPhoto(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    principal.countryCode(),
                    assetId.toString(),
                    stage != null ? stage.name() : MediaStage.REGISTRO.name()
            );

            MediaEvidenceEntity ev = new MediaEvidenceEntity();
            ev.setId(UUID.randomUUID());
            ev.setCountryId(principal.countryId());
            ev.setAssetId(assetId);
            ev.setTicketId(ticketId);
            ev.setStage(stage != null ? stage : MediaStage.REGISTRO);
            ev.setMinioObjectKey(uploaded.objectKey());
            ev.setContentType(uploaded.contentType());
            ev.setOriginalSizeBytes(uploaded.originalSizeBytes());
            ev.setCompressedSizeBytes(uploaded.compressedSizeBytes());
            ev.setSha256Hash(uploaded.sha256Hash());
            ev.setCapturedByUserId(principal.userId());
            ev.setCapturedAt(OffsetDateTime.now());
            ev.setCreatedBy(principal.username());
            ev.setUpdatedBy(principal.username());
            ev = mediaRepository.save(ev);

            if (asset.getPrimaryPhotoKey() == null || ev.getStage() == MediaStage.REGISTRO) {
                asset.setPrimaryPhotoKey(uploaded.objectKey());
                asset.setUpdatedBy(principal.username());
                assetRepository.save(asset);
            }

            auditService.record("MEDIA_UPLOAD", "ASSET", assetId.toString(), Map.of(
                    "stage", ev.getStage().name(),
                    "originalBytes", uploaded.originalSizeBytes(),
                    "compressedBytes", uploaded.compressedSizeBytes(),
                    "processingMs", uploaded.processingMs()
            ));
            return ev;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando foto de evidencia: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadEvidenceBytes(UUID evidenceId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        MediaEvidenceEntity ev = mediaRepository.findByIdAndCountryId(evidenceId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidencia no encontrada"));
        return satelliteClient.fetchMediaBytes(ev.getMinioObjectKey());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocumentBytes(UUID documentId) {
        TenantContext.PrincipalInfo principal = requirePrincipal();
        GeneratedDocumentEntity doc = documentRepository.findByIdAndCountryId(documentId, principal.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acta PDF no encontrada"));
        return satelliteClient.fetchPdfBytes(doc.getMinioObjectKey());
    }

    private void saveEncryptedVault(UUID assetId, TenantContext.PrincipalInfo principal,
                                    String bitlocker, String email, String password, String notes) {
        Map<String, String> plain = new HashMap<>();
        plain.put("bitlocker", bitlocker);
        plain.put("email", email);
        plain.put("password", password);
        plain.put("notes", notes);
        Map<String, String> enc = satelliteClient.encryptVaultEntries(plain);

        AssetVaultEntity vault = vaultRepository.findByAssetIdAndCountryId(assetId, principal.countryId())
                .orElseGet(() -> {
                    AssetVaultEntity v = new AssetVaultEntity();
                    v.setId(UUID.randomUUID());
                    v.setCountryId(principal.countryId());
                    v.setAssetId(assetId);
                    v.setCreatedBy(principal.username());
                    return v;
                });
        vault.setBitlockerRecoveryKeyEnc(enc.get("bitlocker"));
        vault.setDeviceEmailEnc(enc.get("email"));
        vault.setDevicePasswordEnc(enc.get("password"));
        vault.setExtraNotesEnc(enc.get("notes"));
        vault.setUpdatedBy(principal.username());
        vaultRepository.save(vault);
    }

    private AssetEntity requireTenantAsset(UUID assetId) {
        UUID countryId = requirePrincipal().countryId();
        return assetRepository.findByIdAndCountryId(assetId, countryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activo no encontrado en este país"));
    }

    private TenantContext.PrincipalInfo requirePrincipal() {
        TenantContext.PrincipalInfo p = TenantContext.get();
        if (p == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión requerida");
        return p;
    }

    private UUID resolvePersonnelIdForUser(TenantContext.PrincipalInfo principal) {
        UserEntity user = userRepository.findById(principal.userId()).orElse(null);
        if (user == null || user.getNationalId() == null) return null;
        return personnelRepository.findByNationalIdAndCountryId(user.getNationalId(), principal.countryId())
                .map(PersonnelEntity::getId)
                .orElse(null);
    }

    private Map<UUID, String> loadPersonnelLabelMap() {
        Map<UUID, String> map = new HashMap<>();
        for (PersonnelEntity p : personnelRepository.findAll()) {
            map.put(p.getId(), p.displayLabel());
        }
        return map;
    }

    private boolean matchesSearch(AssetEntity a, String search) {
        if (search == null || search.isBlank()) return true;
        String s = search.toLowerCase();
        return a.getAssetNumber().toLowerCase().contains(s)
                || a.getSerialNumber().toLowerCase().contains(s)
                || a.getBrand().toLowerCase().contains(s)
                || a.getModel().toLowerCase().contains(s);
    }

    private boolean hasAnySecret(String a, String b, String c) {
        return (a != null && !a.isBlank()) || (b != null && !b.isBlank()) || (c != null && !c.isBlank());
    }

    private AssetSummaryDto toSummary(AssetEntity a, String assigneeLabel) {
        return new AssetSummaryDto(
                a.getId(), a.getAssetNumber(), a.getEquipmentNumber(), a.getCategory(),
                a.getSubcategory(), a.getBrand(), a.getModel(), a.getSerialNumber(),
                a.getConditionStatus(), a.getStatus(), a.getPhysicalLocation(),
                a.getCurrentAssigneeId(), assigneeLabel, a.getPrimaryPhotoKey(),
                a.getQualityRating(), a.getCreatedAt()
        );
    }
}
