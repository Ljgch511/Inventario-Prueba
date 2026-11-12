export type Role = 'ADMIN' | 'SUPERVISOR' | 'COMMON_USER' | 'MAINTENANCE' | 'ACCOUNTING';

export type AssetCategory = 'ACTIVO' | 'COMPLEMENTO' | 'OBJETO_OFICINA';
export type AssetStatus =
  | 'DISPONIBLE'
  | 'ASIGNADO'
  | 'EN_MANTENIMIENTO'
  | 'DANADO'
  | 'DESINCORPORADO'
  | 'EN_TRANSPORTE';

export type ConditionStatus = 'NUEVO' | 'BUENO' | 'REGULAR' | 'MALO';
export type TicketStatus =
  | 'PENDIENTE_APROBACION'
  | 'APROBADO'
  | 'EN_PROCESO'
  | 'COMPLETADO'
  | 'RECHAZADO'
  | 'CANCELADO';

export type TicketType =
  | 'ASIGNACION'
  | 'DEVOLUCION'
  | 'TRASLADO_TRANSPORTE'
  | 'MANTENIMIENTO'
  | 'ENTRADA_INGRESO'
  | 'SALIDA'
  | 'DESINCORPORACION'
  | 'CONSIGNACION_VENTA'
  | 'CAMBIO_PARTE';

export type DocumentType = 'ASIGNACION' | 'SALIDA' | 'ENTRADA' | 'REPARACION' | 'TRANSPORTE';

export interface CountryDto {
  id: string;
  code: string;
  name: string;
  currencyCode: string;
}

export interface UserSummary {
  id: string;
  username: string;
  email: string;
  fullName: string;
  nationalId: string;
  role: Role;
  countryId: string;
  countryCode: string;
  multiCountryAccess: boolean;
}

export interface LoginResponse {
  token: string;
  user: UserSummary;
  availableCountries: CountryDto[];
}

export interface AssetSummary {
  id: string;
  assetNumber: string;
  equipmentNumber?: string;
  category: AssetCategory;
  subcategory: string;
  brand: string;
  model: string;
  serialNumber: string;
  conditionStatus: ConditionStatus;
  status: AssetStatus;
  physicalLocation: string;
  currentAssigneeId?: string;
  currentAssigneeLabel?: string;
  primaryPhotoKey?: string;
  qualityRating?: number;
  createdAt: string;
}

export interface AssetDetail extends AssetSummary {
  countryId: string;
  version: number;
  processor?: string;
  operatingSystem?: string;
  ramSpec?: string;
  storageRomSpec?: string;
  hasEthernetPort?: boolean;
  bitlockerEnabled: boolean;
  observations?: string;
  specsJson: Record<string, unknown>;
  qrTokenId?: string;
  hasQrPayload: boolean;
  updatedAt: string;
  complements: any[];
  assignments: any[];
  maintenances: any[];
  partChanges: any[];
  tickets: any[];
  movements: any[];
  decommissioning?: any;
  financialRecords: any[];
  mediaEvidences: any[];
  generatedDocuments: any[];
}

export interface AssetVault {
  assetId: string;
  bitlockerRecoveryKey?: string;
  deviceEmail?: string;
  devicePassword?: string;
  extraNotes?: string;
}

export interface TicketRow {
  id: string;
  ticketCode: string;
  ticketType: TicketType;
  status: TicketStatus;
  priority: string;
  title: string;
  reason: string;
  assetId: string;
  assetNumber: string;
  assetSummary: string;
  requesterName: string;
  targetPersonnelLabel?: string;
  approverName?: string;
  transporterName?: string;
  originLocation?: string;
  destinationLocation?: string;
  payloadJson?: Record<string, unknown>;
  approvedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  approvals: any[];
}

export interface PersonnelItem {
  id: string;
  targetType: 'PERSON' | 'OFFICE' | 'GROUP';
  firstName?: string;
  lastName?: string;
  nationalId?: string;
  groupOrOfficeName?: string;
  email?: string;
  department?: string;
}

export interface StoredPartItem {
  id: string;
  partType: 'RAM' | 'SSD' | 'HDD' | 'OTRO';
  capacity: string;
  brand: string;
  model?: string;
  serialNumber: string;
  status: string;
  installedInAssetId?: string;
  physicalLocation?: string;
}
