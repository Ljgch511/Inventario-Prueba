package com.globalr.inventory.modules.asset.domain.model;

/**
 * Clasificación de alto nivel para activos en GlobalR:
 * - ACTIVO_PRINCIPAL: Laptops, PCs, servidores, switches (equipos con serial e info técnica).
 * - COMPLEMENTO: Monitores, mouses, cargadores (equipos que pueden vincularse a un activo principal).
 * - OBJETO_OFICINA: Mobiliario, sillas, escritorios.
 */
public enum AssetCategory {
    ACTIVO_PRINCIPAL,
    COMPLEMENTO,
    OBJETO_OFICINA
}
