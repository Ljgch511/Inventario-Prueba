package com.globalr.inventory.modules.asset.domain.model;

import com.globalr.inventory.modules.asset.domain.exception.InvalidAssetException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de Dominio Puro: Activo.
 * Representa un bien o equipo físico dentro de GlobalR.
 * 
 * Reglas de diseño (Clean Architecture & SOLID):
 * 1. Java puro: sin anotaciones de persistencia (@Entity, @Table) ni de framework (@Component).
 * 2. Invariantes protegidas: no se puede crear un activo en un estado inválido.
 * 3. Encapsulamiento estricto: los cambios de estado se hacen mediante métodos con significado de negocio, no con setters arbitrarios.
 */
public class Asset {

    private final AssetId id;
    private final String assetNumber;
    private final AssetCategory category;
    private final String subcategory;
    private final String brand;
    private final String model;
    private final String serialNumber;
    private final Instant createdAt;

    private AssetStatus status;
    private String physicalLocation;
    private UUID currentAssigneeId;

    private Asset(
            AssetId id,
            String assetNumber,
            AssetCategory category,
            String subcategory,
            String brand,
            String model,
            String serialNumber,
            AssetStatus status,
            String physicalLocation,
            UUID currentAssigneeId,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.assetNumber = validateNotBlank(assetNumber, "El número de activo (assetNumber) es obligatorio");
        this.category = Objects.requireNonNull(category, "La categoría es obligatoria");
        this.subcategory = validateNotBlank(subcategory, "La subcategoría es obligatoria");
        this.brand = validateNotBlank(brand, "La marca es obligatoria");
        this.model = validateNotBlank(model, "El modelo es obligatorio");
        this.serialNumber = validateNotBlank(serialNumber, "El número de serial es obligatorio");
        this.status = Objects.requireNonNull(status, "El estado es obligatorio");
        this.physicalLocation = validateNotBlank(physicalLocation, "La ubicación física es obligatoria");
        this.currentAssigneeId = currentAssigneeId;
        this.createdAt = Objects.requireNonNull(createdAt, "La fecha de creación es obligatoria");
    }

    /**
     * Factory method para crear un nuevo activo en el inventario.
     * Nace siempre en estado DISPONIBLE y sin asignación.
     */
    public static Asset createNew(
            String assetNumber,
            AssetCategory category,
            String subcategory,
            String brand,
            String model,
            String serialNumber,
            String physicalLocation
    ) {
        return new Asset(
                AssetId.generate(),
                assetNumber,
                category,
                subcategory,
                brand,
                model,
                serialNumber,
                AssetStatus.DISPONIBLE,
                physicalLocation,
                null,
                Instant.now()
        );
    }

    /**
     * Factory method para reconstituir un activo desde la persistencia (cuando se lee de base de datos).
     */
    public static Asset reconstitute(
            AssetId id,
            String assetNumber,
            AssetCategory category,
            String subcategory,
            String brand,
            String model,
            String serialNumber,
            AssetStatus status,
            String physicalLocation,
            UUID currentAssigneeId,
            Instant createdAt
    ) {
        return new Asset(
                id,
                assetNumber,
                category,
                subcategory,
                brand,
                model,
                serialNumber,
                status,
                physicalLocation,
                currentAssigneeId,
                createdAt
        );
    }

    // =========================================================================
    // Métodos de Negocio (Transiciones de Estado Controladas)
    // =========================================================================

    public void assignTo(UUID personnelId) {
        if (personnelId == null) {
            throw new InvalidAssetException("No se puede asignar un activo a un responsable nulo");
        }
        if (this.status != AssetStatus.DISPONIBLE) {
            throw new InvalidAssetException("Solo se pueden asignar activos en estado DISPONIBLE. Estado actual: " + this.status);
        }
        this.status = AssetStatus.ASIGNADO;
        this.currentAssigneeId = personnelId;
    }

    public void releaseAssignment() {
        if (this.status != AssetStatus.ASIGNADO) {
            throw new InvalidAssetException("Solo se puede desasignar un activo que se encuentre en estado ASIGNADO");
        }
        this.status = AssetStatus.DISPONIBLE;
        this.currentAssigneeId = null;
    }

    public void sendToMaintenance() {
        if (this.status == AssetStatus.DESINCORPORADO) {
            throw new InvalidAssetException("Un activo desincorporado no puede enviarse a mantenimiento");
        }
        this.status = AssetStatus.EN_MANTENIMIENTO;
    }

    public void completeMaintenance() {
        if (this.status != AssetStatus.EN_MANTENIMIENTO) {
            throw new InvalidAssetException("Solo se puede completar mantenimiento a un activo en estado EN_MANTENIMIENTO");
        }
        // Si tenía un asignatario previo, vuelve a asignado; si no, queda disponible
        this.status = (this.currentAssigneeId != null) ? AssetStatus.ASIGNADO : AssetStatus.DISPONIBLE;
    }

    public void decommission() {
        if (this.status == AssetStatus.DESINCORPORADO) {
            throw new InvalidAssetException("El activo ya se encuentra desincorporado");
        }
        this.status = AssetStatus.DESINCORPORADO;
        this.currentAssigneeId = null;
    }

    public void updateLocation(String newLocation) {
        this.physicalLocation = validateNotBlank(newLocation, "La nueva ubicación no puede estar vacía");
    }

    // =========================================================================
    // Getters de lectura (inmutables hacia afuera)
    // =========================================================================

    public AssetId getId() { return id; }
    public String getAssetNumber() { return assetNumber; }
    public AssetCategory getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public AssetStatus getStatus() { return status; }
    public String getPhysicalLocation() { return physicalLocation; }
    public UUID getCurrentAssigneeId() { return currentAssigneeId; }
    public Instant getCreatedAt() { return createdAt; }

    // =========================================================================
    // Validaciones auxiliares
    // =========================================================================

    private static String validateNotBlank(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidAssetException(errorMessage);
        }
        return value.trim();
    }
}
