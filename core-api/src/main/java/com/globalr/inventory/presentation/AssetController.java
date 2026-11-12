package com.globalr.inventory.presentation;

import com.globalr.inventory.application.AssetService;
import com.globalr.inventory.application.dto.ApiDtos.*;
import com.globalr.inventory.domain.enums.DomainEnums.AssetCategory;
import com.globalr.inventory.domain.enums.DomainEnums.AssetStatus;
import com.globalr.inventory.domain.enums.DomainEnums.MediaStage;
import com.globalr.inventory.domain.model.AssetComplementEntity;
import com.globalr.inventory.domain.model.MediaEvidenceEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public List<AssetSummaryDto> list(
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String search
    ) {
        return assetService.listAssets(category, status, search);
    }

    @GetMapping("/{assetId}")
    public AssetDetailDto getDetail(@PathVariable UUID assetId) {
        return assetService.getAssetDetail(assetId);
    }

    @PostMapping
    public AssetDetailDto create(@Valid @RequestBody CreateAssetRequest request) {
        return assetService.createAsset(request);
    }

    @PatchMapping("/{assetId}")
    public AssetDetailDto update(@PathVariable UUID assetId, @RequestBody UpdateAssetRequest request) {
        return assetService.updateAsset(assetId, request);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID assetId) {
        assetService.softDeleteAsset(assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assetId}/qr")
    public Map<String, Object> getQr(@PathVariable UUID assetId) {
        return assetService.getAssetQr(assetId);
    }

    @PostMapping("/qr/scan")
    public Map<String, Object> scanQr(@RequestBody Map<String, String> body) {
        return assetService.scanAndVerifyQr(body.get("encryptedPayload"));
    }

    @PostMapping("/{assetId}/complements")
    public AssetComplementEntity addComplement(
            @PathVariable UUID assetId,
            @Valid @RequestBody AddComplementRequest request
    ) {
        return assetService.addComplement(assetId, request);
    }

    @GetMapping("/{assetId}/vault")
    public AssetVaultDto getVault(@PathVariable UUID assetId) {
        return assetService.getVault(assetId);
    }

    @PutMapping("/{assetId}/vault")
    public AssetVaultDto upsertVault(@PathVariable UUID assetId, @RequestBody AssetVaultDto dto) {
        return assetService.upsertVault(assetId, dto);
    }

    @PostMapping(value = "/{assetId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaEvidenceEntity uploadMedia(
            @PathVariable UUID assetId,
            @RequestParam(required = false) UUID ticketId,
            @RequestParam(defaultValue = "REGISTRO") MediaStage stage,
            @RequestPart("file") MultipartFile file
    ) {
        return assetService.uploadEvidence(assetId, ticketId, stage, file);
    }

    @GetMapping("/media/{evidenceId}/content")
    public ResponseEntity<byte[]> downloadMedia(@PathVariable UUID evidenceId) {
        byte[] bytes = assetService.downloadEvidenceBytes(evidenceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(bytes);
    }
}
