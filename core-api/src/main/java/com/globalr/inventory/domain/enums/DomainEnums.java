package com.globalr.inventory.domain.enums;

public final class DomainEnums {
    private DomainEnums() {}

    public enum AssetCategory { ACTIVO, COMPLEMENTO, OBJETO_OFICINA }
    public enum AssetStatus { DISPONIBLE, ASIGNADO, EN_MANTENIMIENTO, DANADO, DESINCORPORADO, EN_TRANSPORTE }
    public enum ConditionStatus { NUEVO, BUENO, REGULAR, MALO }
    public enum AssigneeTargetType { PERSON, OFFICE, GROUP }
    public enum PartType { RAM, SSD, HDD, OTRO }
    public enum StoredPartStatus { EN_ALMACEN, INSTALADA, DANADA, DESINCORPORADA }

    public enum TicketType {
        ASIGNACION, DEVOLUCION, TRASLADO_TRANSPORTE, MANTENIMIENTO,
        ENTRADA_INGRESO, SALIDA, DESINCORPORACION, CONSIGNACION_VENTA, CAMBIO_PARTE
    }

    public enum TicketStatus {
        PENDIENTE_APROBACION, APROBADO, EN_PROCESO, COMPLETADO, RECHAZADO, CANCELADO
    }

    public enum TicketPriority { BAJA, MEDIA, ALTA, CRITICA }
    public enum MovementType { ENTRADA, SALIDA, TRASLADO }
    public enum EntrySource { COMPRA, REGALO_PROVEEDOR, GARANTIA, DEVOLUCION, OTRO }
    public enum MaintenanceMode { INTERNO, EXTERNO }
    public enum MaintenanceStatus { ABIERTA, EN_REPARACION, FINALIZADA }
    public enum DecommissionDisposition { DONADO, DESECHADO }

    public enum AcquisitionType {
        COMPRA_NACIONAL, COMPRA_INTERNACIONAL, LEASING, ALQUILER,
        ARRENDAMIENTO_OTORGADO, ARRENDAMIENTO_RECIBIDO, CONSIGNACION, REGALO_PROVEEDOR
    }

    public enum DocumentType { ASIGNACION, SALIDA, ENTRADA, REPARACION, TRANSPORTE }
    public enum MediaStage { REGISTRO, ASIGNACION, RECEPCION, ENTRADA, SALIDA, MANTENIMIENTO }
}
