package com.globalr.inventory.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Cryptographic engine using BouncyCastle for asymmetric RSA signing
 * and AES-256-GCM symmetric encryption of QR labels and Asset Vault secrets.
 */
@ApplicationScoped
public class CryptoService {

    private static final String TOKEN_PREFIX = "GLR1";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    @ConfigProperty(name = "crypto.aes.master-key")
    String configuredMasterKey;

    @ConfigProperty(name = "crypto.keys.dir", defaultValue = "/app/keys")
    String keysDir;

    @Inject
    ObjectMapper objectMapper;

    private SecretKey aesKey;
    private KeyPair rsaKeyPair;

    @PostConstruct
    void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            this.aesKey = deriveAes256Key(configuredMasterKey);
            this.rsaKeyPair = loadOrGenerateRsaKeyPair(Path.of(keysDir));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CryptoService", e);
        }
    }

    public CryptoDtos.QrSignResponse signAndEncryptQr(CryptoDtos.QrSignRequest req) {
        long startNs = System.nanoTime();
        try {
            Map<String, String> claims = Map.of(
                    "assetId", safe(req.assetId()),
                    "assetNumber", safe(req.assetNumber()),
                    "serialNumber", safe(req.serialNumber()),
                    "countryCode", safe(req.countryCode()),
                    "qrTokenId", safe(req.qrTokenId()),
                    "issuedAt", Instant.now().toString()
            );
            byte[] jsonBytes = objectMapper.writeValueAsBytes(claims);
            String cipherB64 = encryptAesGcmUrlSafe(jsonBytes);

            Signature signature = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            signature.initSign(rsaKeyPair.getPrivate());
            signature.update(cipherB64.getBytes(StandardCharsets.UTF_8));
            String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());

            String payload = TOKEN_PREFIX + "." + cipherB64 + "." + sigB64;
            String qrPngBase64 = renderQrPngBase64(payload, 280);
            long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);
            return new CryptoDtos.QrSignResponse(payload, qrPngBase64, elapsedMs);
        } catch (Exception e) {
            throw new IllegalStateException("Error signing QR payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    public CryptoDtos.QrVerifyResponse verifyAndDecryptQr(String encryptedPayload) {
        long startNs = System.nanoTime();
        try {
            if (encryptedPayload == null || encryptedPayload.isBlank()) {
                return invalidResponse(startNs, "Payload vacío");
            }
            String[] parts = encryptedPayload.trim().split("\\.");
            if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
                return invalidResponse(startNs, "Formato de código QR inválido");
            }

            String cipherB64 = parts[1];
            byte[] sigBytes = Base64.getUrlDecoder().decode(parts[2]);

            Signature verifier = Signature.getInstance("SHA256withRSA", BouncyCastleProvider.PROVIDER_NAME);
            verifier.initVerify(rsaKeyPair.getPublic());
            verifier.update(cipherB64.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(sigBytes)) {
                return invalidResponse(startNs, "Firma digital RSA inválida");
            }

            byte[] plainJson = decryptAesGcmUrlSafe(cipherB64);
            Map<String, String> claims = objectMapper.readValue(plainJson, Map.class);
            long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);

            return new CryptoDtos.QrVerifyResponse(
                    true,
                    claims.get("assetId"),
                    claims.get("assetNumber"),
                    claims.get("serialNumber"),
                    claims.get("countryCode"),
                    claims.get("qrTokenId"),
                    claims.get("issuedAt"),
                    elapsedMs,
                    null
            );
        } catch (Exception e) {
            return invalidResponse(startNs, "Error verificando QR: " + e.getMessage());
        }
    }

    public Map<String, String> encryptMap(Map<String, String> plainMap) {
        Map<String, String> result = new HashMap<>();
        if (plainMap == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : plainMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                result.put(entry.getKey(), null);
            } else {
                result.put(entry.getKey(), encryptAesGcmUrlSafe(entry.getValue().getBytes(StandardCharsets.UTF_8)));
            }
        }
        return result;
    }

    public Map<String, String> decryptMap(Map<String, String> encMap) {
        Map<String, String> result = new HashMap<>();
        if (encMap == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : encMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                result.put(entry.getKey(), null);
            } else {
                byte[] plain = decryptAesGcmUrlSafe(entry.getValue());
                result.put(entry.getKey(), new String(plain, StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    public String renderQrPngBase64(String content, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String encryptAesGcmUrlSafe(byte[] plainBytes) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    private byte[] decryptAesGcmUrlSafe(String cipherB64) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(cipherB64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }

    private SecretKey deriveAes256Key(String raw) throws NoSuchAlgorithmException {
        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            if (decoded.length == 32) {
                return new SecretKeySpec(decoded, "AES");
            }
        } catch (IllegalArgumentException ignored) {
        }
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hashed = sha.digest(raw.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hashed, "AES");
    }

    private KeyPair loadOrGenerateRsaKeyPair(Path dir) throws Exception {
        Files.createDirectories(dir);
        Path privPath = dir.resolve("qr_rsa_private.der");
        Path pubPath = dir.resolve("qr_rsa_public.der");
        KeyFactory kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);

        if (Files.exists(privPath) && Files.exists(pubPath)) {
            PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(privPath)));
            PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(pubPath)));
            return new KeyPair(pub, priv);
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();
        Files.write(privPath, kp.getPrivate().getEncoded());
        Files.write(pubPath, kp.getPublic().getEncoded());
        return kp;
    }

    private CryptoDtos.QrVerifyResponse invalidResponse(long startNs, String error) {
        long elapsedMs = Math.max(1L, (System.nanoTime() - startNs) / 1_000_000L);
        return new CryptoDtos.QrVerifyResponse(false, null, null, null, null, null, null, elapsedMs, error);
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }
}
