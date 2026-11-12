package com.globalr.inventory.crypto;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/crypto")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CryptoResource {

    @Inject
    CryptoService cryptoService;

    @POST
    @Path("/qr/sign")
    public CryptoDtos.QrSignResponse signQr(CryptoDtos.QrSignRequest request) {
        return cryptoService.signAndEncryptQr(request);
    }

    @POST
    @Path("/qr/verify")
    public CryptoDtos.QrVerifyResponse verifyQr(CryptoDtos.QrVerifyRequest request) {
        return cryptoService.verifyAndDecryptQr(request != null ? request.encryptedPayload() : null);
    }

    @POST
    @Path("/vault/encrypt")
    public CryptoDtos.VaultEncryptResponse encryptVault(CryptoDtos.VaultEncryptRequest request) {
        return new CryptoDtos.VaultEncryptResponse(cryptoService.encryptMap(request != null ? request.entries() : null));
    }

    @POST
    @Path("/vault/decrypt")
    public CryptoDtos.VaultDecryptResponse decryptVault(CryptoDtos.VaultDecryptRequest request) {
        return new CryptoDtos.VaultDecryptResponse(cryptoService.decryptMap(request != null ? request.encryptedEntries() : null));
    }

    @POST
    @Path("/qr/render")
    public CryptoDtos.QrRenderResponse renderQr(CryptoDtos.QrRenderRequest request) throws Exception {
        String b64 = cryptoService.renderQrPngBase64(request.encryptedPayload(), 280);
        return new CryptoDtos.QrRenderResponse(b64);
    }
}
