package com.globalr.inventory.modules.asset.domain;

import com.globalr.inventory.modules.asset.domain.exception.InvalidAssetException;
import com.globalr.inventory.modules.asset.domain.model.Asset;
import com.globalr.inventory.modules.asset.domain.model.AssetCategory;
import com.globalr.inventory.modules.asset.domain.model.AssetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias del Dominio: Entidad Asset")
class AssetTest {

    @Test
    @DisplayName("Debe crear un activo válido en estado DISPONIBLE")
    void shouldCreateValidAsset() {
        Asset asset = Asset.createNew(
                "GLR-VE-0001",
                AssetCategory.ACTIVO_PRINCIPAL,
                "LAPTOP",
                "Dell",
                "Latitude 5420",
                "SN-ABC-12345",
                "Almacén Principal Caracas"
        );

        assertThat(asset.getId()).isNotNull();
        assertThat(asset.getAssetNumber()).isEqualTo("GLR-VE-0001");
        assertThat(asset.getCategory()).isEqualTo(AssetCategory.ACTIVO_PRINCIPAL);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.DISPONIBLE);
        assertThat(asset.getCurrentAssigneeId()).isNull();
        assertThat(asset.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe lanzar excepción si el serial o campos obligatorios están en blanco")
    void shouldThrowExceptionWhenMandatoryFieldsAreBlank() {
        assertThatThrownBy(() -> Asset.createNew(
                "GLR-VE-0001",
                AssetCategory.ACTIVO_PRINCIPAL,
                "LAPTOP",
                "Dell",
                "Latitude 5420",
                "   ", // Serial en blanco
                "Almacén Caracas"
        ))
        .isInstanceOf(InvalidAssetException.class)
        .hasMessageContaining("serial es obligatorio");
    }

    @Test
    @DisplayName("Debe permitir asignar un activo disponible a un responsable")
    void shouldAssignAssetSuccessfully() {
        Asset asset = createSampleAsset();
        UUID personnelId = UUID.randomUUID();

        asset.assignTo(personnelId);

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.ASIGNADO);
        assertThat(asset.getCurrentAssigneeId()).isEqualTo(personnelId);
    }

    @Test
    @DisplayName("No debe permitir asignar un activo si ya está asignado o en mantenimiento")
    void shouldPreventAssigningNonAvailableAsset() {
        Asset asset = createSampleAsset();
        asset.assignTo(UUID.randomUUID());

        // Intentar reasignar sin haberlo desasignado antes
        assertThatThrownBy(() -> asset.assignTo(UUID.randomUUID()))
                .isInstanceOf(InvalidAssetException.class)
                .hasMessageContaining("Solo se pueden asignar activos en estado DISPONIBLE");
    }

    @Test
    @DisplayName("Debe permitir desasignar un activo previamente asignado")
    void shouldReleaseAssignment() {
        Asset asset = createSampleAsset();
        asset.assignTo(UUID.randomUUID());

        asset.releaseAssignment();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.DISPONIBLE);
        assertThat(asset.getCurrentAssigneeId()).isNull();
    }

    @Test
    @DisplayName("No debe permitir enviar a mantenimiento un activo desincorporado")
    void shouldPreventMaintenanceOnDecommissionedAsset() {
        Asset asset = createSampleAsset();
        asset.decommission();

        assertThatThrownBy(asset::sendToMaintenance)
                .isInstanceOf(InvalidAssetException.class)
                .hasMessageContaining("desincorporado no puede enviarse a mantenimiento");
    }

    private Asset createSampleAsset() {
        return Asset.createNew(
                "GLR-VE-0001",
                AssetCategory.ACTIVO_PRINCIPAL,
                "LAPTOP",
                "Lenovo",
                "ThinkPad T14",
                "SN-LEN-9988",
                "Sede Caracas"
        );
    }
}
