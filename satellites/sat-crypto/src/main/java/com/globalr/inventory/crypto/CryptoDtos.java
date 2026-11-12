package com.globalr.inventory.crypto;

import java.util.Map;

public class CryptoDtos {

    public record QrSignRequest(
            String assetId,
            String assetNumber,
            String serialNumber,
            String countryCode,
            String qrTokenId
    ) {}

    public record QrSignResponse(
            String encryptedPayload,
            String qrPngBase64,
            long processingMs
    ) {}

    public record QrVerifyRequest(
            String encryptedPayload
    ) {}

    public record QrVerifyResponse(
            boolean valid,
            String assetId,
            String assetNumber,
            String serialNumber,
            String countryCode,
            String qrTokenId,
            String issuedAt,
            long processingMs,
            String error
    ) {}

    public record VaultEncryptRequest(
            Map<String, String> entries
    ) {}

    public record VaultEncryptResponse(
            Map<String, String> encryptedEntries
    ) {}

    public record VaultDecryptRequest(
            Map<String, String> encryptedEntries
    ) {}

    public record VaultDecryptResponse(
            Map<String, String> entries
    ) {}

    public record QrRenderRequest(
            String encryptedPayload
    ) {}

    public record QrRenderResponse(
            String qrPngBase64
    ) {}
}
