package com.globalr.inventory.modules.asset.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object inmutable que representa el identificador único de un activo.
 * Evita confusiones de tipos (evita pasar por error un userId donde va un assetId).
 */
public record AssetId(UUID value) {

    public AssetId {
        Objects.requireNonNull(value, "El valor de AssetId no puede ser nulo");
    }

    public static AssetId generate() {
        return new AssetId(UUID.randomUUID());
    }

    public static AssetId fromString(String uuidStr) {
        return new AssetId(UUID.fromString(uuidStr));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
