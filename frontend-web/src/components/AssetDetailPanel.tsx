import React, { useEffect, useState } from 'react';
import {
  Camera, FileText, KeyRound, Plus, QrCode, ShieldAlert, Trash2, Upload, X
} from 'lucide-react';
import { api } from '../api/client';
import { AssetDetail, AssetVault, DocumentType, Role } from '../types';

interface Props {
  assetId: string;
  role: Role;
  onClose: () => void;
  onDeleted: () => void;
  onQuickTicket: (assetId: string) => void;
}

const statusBadge = (status: string) => {
  switch (status) {
    case 'DISPONIBLE': return 'badge badge-success';
    case 'ASIGNADO': return 'badge badge-info';
    case 'EN_MANTENIMIENTO':
    case 'EN_TRANSPORTE': return 'badge badge-warn';
    case 'DANADO':
    case 'DESINCORPORADO': return 'badge badge-danger';
    default: return 'badge badge-neutral';
  }
};

export const AssetDetailPanel: React.FC<Props> = ({
  assetId, role, onClose, onDeleted, onQuickTicket
}) => {
  const [detail, setDetail] = useState<AssetDetail | null>(null);
  const [tab, setTab] = useState<'resumen' | 'historicos' | 'complementos' | 'evidencias' | 'actas' | 'vault'>('resumen');
  const [qrInfo, setQrInfo] = useState<any>(null);
  const [qrVerifyResult, setQrVerifyResult] = useState<any>(null);
  const [vault, setVault] = useState<AssetVault | null>(null);
  const [vaultForm, setVaultForm] = useState({ bitlockerRecoveryKey: '', deviceEmail: '', devicePassword: '', extraNotes: '' });
  const [photoStage, setPhotoStage] = useState('REGISTRO');
  const [photoUrls, setPhotoUrls] = useState<Record<string, string>>({});
  const [compForm, setCompForm] = useState({ complementType: 'MONITOR', brand: '', model: '', serialNumber: '', notes: '' });
  const [actaForm, setActaForm] = useState<{ documentType: DocumentType; reason: string }>({
    documentType: 'ASIGNACION',
    reason: 'Constancia operativa de activo'
  });
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const isPrivileged = role === 'ADMIN' || role === 'SUPERVISOR';
  const isCommon = role === 'COMMON_USER';

  const load = async () => {
    try {
      setErr(null);
      const d = await api.getAssetDetail(assetId);
      setDetail(d);
      if (!isCommon) {
        const q = await api.getAssetQr(assetId);
        setQrInfo(q);
      }
    } catch (e: any) {
      setErr(e.message);
    }
  };

  useEffect(() => {
    load();
    return () => {
      Object.values(photoUrls).forEach((u) => URL.revokeObjectURL(u));
    };
  }, [assetId]);

  const handleLoadVault = async () => {
    try {
      setErr(null);
      const v = await api.getVault(assetId);
      setVault(v);
      setVaultForm({
        bitlockerRecoveryKey: v.bitlockerRecoveryKey || '',
        deviceEmail: v.deviceEmail || '',
        devicePassword: v.devicePassword || '',
        extraNotes: v.extraNotes || ''
      });
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleSaveVault = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const updated = await api.upsertVault(assetId, { assetId, ...vaultForm });
      setVault(updated);
      setMsg('Bóveda cifrada actualizada y auditada (ISO 27001).');
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleVerifyQrInstant = async () => {
    if (!qrInfo?.encryptedPayload) return;
    try {
      const res = await api.scanQr(qrInfo.encryptedPayload);
      setQrVerifyResult(res);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleUploadPhoto = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      setErr(null);
      await api.uploadEvidence(assetId, photoStage, file);
      setMsg('Evidencia comprimida vía sat-media y guardada en MinIO.');
      await load();
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  const handlePreviewPhoto = async (evId: string) => {
    if (photoUrls[evId]) return;
    try {
      const url = await api.fetchBlobUrl(`/assets/media/${evId}/content`);
      setPhotoUrls((prev) => ({ ...prev, [evId]: url }));
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  const handleAddComplement = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.addComplement(assetId, compForm);
      setCompForm({ complementType: 'MONITOR', brand: '', model: '', serialNumber: '', notes: '' });
      setMsg('Complemento asociado correctamente.');
      await load();
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  const handleGenerateActa = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const doc = await api.generateActa({
        assetId,
        documentType: actaForm.documentType,
        reason: actaForm.reason
      });
      setMsg(`Acta ${doc.documentNumber} generada en PDF (sat-pdf + MinIO).`);
      await load();
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  const handleDownloadPdf = async (docId: string) => {
    try {
      const url = await api.fetchBlobUrl(`/documents/${docId}/download`);
      window.open(url, '_blank');
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  const handleSoftDelete = async () => {
    if (!confirm('¿Aplicar borrado lógico (Soft Delete) a este activo? El registro histórico permanecerá inmutable.')) return;
    try {
      await api.softDeleteAsset(assetId);
      onDeleted();
    } catch (ex: any) {
      setErr(ex.message);
    }
  };

  if (!detail) {
    return (
      <div className="modal-backdrop">
        <div className="modal-card">Cargando ficha del activo...</div>
      </div>
    );
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card" style={{ maxWidth: 960 }}>
        <div className="modal-header">
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <h3 style={{ margin: 0 }}>{detail.assetNumber} — {detail.brand} {detail.model}</h3>
              <span className={statusBadge(detail.status)}>{detail.status}</span>
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 2 }}>
              {detail.category} / {detail.subcategory} · Serial: <strong>{detail.serialNumber}</strong> · Ubicación: {detail.physicalLocation}
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-primary btn-sm" onClick={() => onQuickTicket(detail.id)}>
              + Ticket sobre activo
            </button>
            {role === 'ADMIN' && (
              <button className="btn btn-danger btn-sm" onClick={handleSoftDelete} title="Borrado Lógico">
                <Trash2 size={14} />
              </button>
            )}
            <button className="btn btn-sm" onClick={onClose}><X size={16} /></button>
          </div>
        </div>

        {msg && <div className="alert alert-info">{msg}</div>}
        {err && <div className="alert alert-error">{err}</div>}

        <div className="tabs">
          <button className={`tab-btn ${tab === 'resumen' ? 'active' : ''}`} onClick={() => setTab('resumen')}>
            Ficha & QR Cifrado
          </button>
          {!isCommon && (
            <>
              <button className={`tab-btn ${tab === 'historicos' ? 'active' : ''}`} onClick={() => setTab('historicos')}>
                Históricos Cruzados ({detail.assignments.length + detail.maintenances.length + detail.partChanges.length + detail.tickets.length})
              </button>
              <button className={`tab-btn ${tab === 'complementos' ? 'active' : ''}`} onClick={() => setTab('complementos')}>
                Complementos ({detail.complements.length})
              </button>
              <button className={`tab-btn ${tab === 'evidencias' ? 'active' : ''}`} onClick={() => setTab('evidencias')}>
                Fotos ({detail.mediaEvidences.length})
              </button>
              <button className={`tab-btn ${tab === 'actas' ? 'active' : ''}`} onClick={() => setTab('actas')}>
                Actas PDF ({detail.generatedDocuments.length})
              </button>
            </>
          )}
          {isPrivileged && (
            <button
              className={`tab-btn ${tab === 'vault' ? 'active' : ''}`}
              onClick={() => { setTab('vault'); handleLoadVault(); }}
            >
              <KeyRound size={13} style={{ marginRight: 4 }} />
              Bóveda Claves
            </button>
          )}
        </div>

        {tab === 'resumen' && (
          <div className="grid-2">
            <div className="card">
              <h4 className="card-title">Especificaciones del Activo</h4>
              <table className="data-table">
                <tbody>
                  <tr><th>N° Equipo</th><td>{detail.equipmentNumber || '-'}</td></tr>
                  <tr><th>Condición</th><td>{detail.conditionStatus}</td></tr>
                  <tr><th>Asignado a</th><td>{detail.currentAssigneeLabel || 'Sin asignar'}</td></tr>
                  <tr><th>Procesador / OS</th><td>{detail.processor || '-'} / {detail.operatingSystem || '-'}</td></tr>
                  <tr><th>RAM / ROM</th><td>{detail.ramSpec || '-'} / {detail.storageRomSpec || '-'}</td></tr>
                  <tr><th>Puerto Ethernet</th><td>{detail.hasEthernetPort == null ? '-' : detail.hasEthernetPort ? 'Sí' : 'No'}</td></tr>
                  <tr><th>BitLocker</th><td>{detail.bitlockerEnabled ? 'Habilitado' : 'No'}</td></tr>
                  {!isCommon && <tr><th>Observaciones</th><td>{detail.observations || '-'}</td></tr>}
                  <tr>
                    <th>Specs JSONB</th>
                    <td><code>{JSON.stringify(detail.specsJson || {})}</code></td>
                  </tr>
                </tbody>
              </table>
            </div>

            {!isCommon && (
              <div className="card">
                <h4 className="card-title">
                  <QrCode size={16} style={{ marginRight: 6, verticalAlign: 'text-bottom' }} />
                  Etiqueta QR Cifrada (RSA + AES-256)
                </h4>
                {qrInfo?.qrPngBase64 ? (
                  <div style={{ textAlign: 'center' }}>
                    <img
                      src={`data:image/png;base64,${qrInfo.qrPngBase64}`}
                      alt="QR Cifrado"
                      style={{ width: 170, height: 170, border: '1px solid #e2e8f0', borderRadius: 6 }}
                    />
                    <div style={{ fontSize: 11, color: '#64748b', marginTop: 6, wordBreak: 'break-all' }}>
                      Token: <code>{qrInfo.qrTokenId}</code>
                    </div>
                    <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 10 }}>
                      <button className="btn btn-sm" onClick={() => navigator.clipboard?.writeText(qrInfo.encryptedPayload)}>
                        Copiar Payload Cifrado
                      </button>
                      <button className="btn btn-primary btn-sm" onClick={handleVerifyQrInstant}>
                        Verificar Firma (&lt;1s)
                      </button>
                    </div>
                    {qrVerifyResult && (
                      <div className="alert alert-info" style={{ marginTop: 10, textAlign: 'left' }}>
                        Firma RSA válida · Descifrado en <strong>{qrVerifyResult.processingMs} ms</strong> · Activo: {qrVerifyResult.asset?.assetNumber}
                      </div>
                    )}
                  </div>
                ) : (
                  <p>Generando código QR...</p>
                )}
              </div>
            )}
          </div>
        )}

        {tab === 'historicos' && !isCommon && (
          <div style={{ display: 'grid', gap: 14 }}>
            <div className="card">
              <h4 className="card-title">Histórico de Asignaciones</h4>
              <table className="data-table">
                <thead><tr><th>Fecha Asignación</th><th>Devolución</th><th>Motivo</th><th>Estado</th></tr></thead>
                <tbody>
                  {detail.assignments.length === 0 ? <tr><td colSpan={4}>Sin asignaciones</td></tr> :
                    detail.assignments.map((a: any) => (
                      <tr key={a.id}>
                        <td>{new Date(a.assignedAt).toLocaleString()}</td>
                        <td>{a.returnedAt ? new Date(a.returnedAt).toLocaleString() : 'Vigente'}</td>
                        <td>{a.reason}</td>
                        <td>{a.assetStatusAtAssign}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <div className="card">
              <h4 className="card-title">Histórico de Mantenimiento y Fallas</h4>
              <table className="data-table">
                <thead><tr><th>Fecha</th><th>Modo</th><th>Técnico/Prov.</th><th>Falla</th><th>Trabajo Realizado</th><th>Estado</th></tr></thead>
                <tbody>
                  {detail.maintenances.length === 0 ? <tr><td colSpan={6}>Sin mantenimientos</td></tr> :
                    detail.maintenances.map((m: any) => (
                      <tr key={m.id}>
                        <td>{new Date(m.startedAt).toLocaleDateString()}</td>
                        <td>{m.maintenanceMode}</td>
                        <td>{m.technicianName || m.providerName || '-'}</td>
                        <td>{m.failureDescription}</td>
                        <td>{m.workPerformed || '-'}</td>
                        <td><span className="badge badge-info">{m.status}</span></td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <div className="grid-2">
              <div className="card">
                <h4 className="card-title">Histórico de Partes Cambiadas</h4>
                <table className="data-table">
                  <thead><tr><th>Fecha</th><th>Retirada</th><th>Instalada</th><th>Motivo</th></tr></thead>
                  <tbody>
                    {detail.partChanges.length === 0 ? <tr><td colSpan={4}>Sin cambios de partes</td></tr> :
                      detail.partChanges.map((p: any) => (
                        <tr key={p.id}>
                          <td>{new Date(p.changedAt).toLocaleDateString()}</td>
                          <td>{p.removedPartDesc || '-'}</td>
                          <td>{p.installedPartDesc}</td>
                          <td>{p.reason}</td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>

              <div className="card">
                <h4 className="card-title">Tickets Vinculados</h4>
                <table className="data-table">
                  <thead><tr><th>Código</th><th>Tipo</th><th>Título</th><th>Estado</th></tr></thead>
                  <tbody>
                    {detail.tickets.length === 0 ? <tr><td colSpan={4}>Sin tickets</td></tr> :
                      detail.tickets.map((t: any) => (
                        <tr key={t.id}>
                          <td><strong>{t.ticketCode}</strong></td>
                          <td>{t.ticketType}</td>
                          <td>{t.title}</td>
                          <td><span className="badge badge-neutral">{t.status}</span></td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>

            {detail.decommissioning && (
              <div className="alert alert-error">
                <strong>Desincorporación ({detail.decommissioning.disposition}):</strong> {detail.decommissioning.reason} — Uso de partes: {detail.decommissioning.partsUsageNotes}
              </div>
            )}
          </div>
        )}

        {tab === 'complementos' && !isCommon && (
          <div style={{ display: 'grid', gap: 14 }}>
            <table className="data-table">
              <thead><tr><th>Tipo</th><th>Marca</th><th>Modelo</th><th>Serial</th><th>Notas</th></tr></thead>
              <tbody>
                {detail.complements.length === 0 ? <tr><td colSpan={5}>Sin complementos asociados</td></tr> :
                  detail.complements.map((c: any) => (
                    <tr key={c.id}>
                      <td>{c.complementType}</td><td>{c.brand}</td><td>{c.model}</td><td>{c.serialNumber}</td><td>{c.notes || '-'}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
            {isPrivileged && (
              <form onSubmit={handleAddComplement} className="card form-grid">
                <div className="field"><label>Tipo</label><input className="input" value={compForm.complementType} onChange={(e) => setCompForm({ ...compForm, complementType: e.target.value })} required /></div>
                <div className="field"><label>Marca</label><input className="input" value={compForm.brand} onChange={(e) => setCompForm({ ...compForm, brand: e.target.value })} required /></div>
                <div className="field"><label>Modelo</label><input className="input" value={compForm.model} onChange={(e) => setCompForm({ ...compForm, model: e.target.value })} required /></div>
                <div className="field"><label>Serial</label><input className="input" value={compForm.serialNumber} onChange={(e) => setCompForm({ ...compForm, serialNumber: e.target.value })} required /></div>
                <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button className="btn btn-primary" type="submit"><Plus size={14} /> Asociar Complemento</button>
                </div>
              </form>
            )}
          </div>
        )}

        {tab === 'evidencias' && !isCommon && (
          <div style={{ display: 'grid', gap: 14 }}>
            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <Camera size={18} color="#0f4c81" />
              <select className="select" style={{ width: 180 }} value={photoStage} onChange={(e) => setPhotoStage(e.target.value)}>
                <option value="REGISTRO">Registro</option>
                <option value="ASIGNACION">Asignación</option>
                <option value="RECEPCION">Recepción</option>
                <option value="ENTRADA">Entrada</option>
                <option value="SALIDA">Salida</option>
                <option value="MANTENIMIENTO">Mantenimiento</option>
              </select>
              <label className="btn btn-primary btn-sm" style={{ cursor: 'pointer' }}>
                <Upload size={14} /> Subir y Comprimir Foto
                <input type="file" accept="image/*" hidden onChange={handleUploadPhoto} />
              </label>
            </div>
            <div className="grid-2">
              {detail.mediaEvidences.map((ev: any) => (
                <div className="card" key={ev.id}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span className="badge badge-info">{ev.stage}</span>
                    <span style={{ fontSize: 11, color: '#64748b' }}>
                      {(ev.originalSizeBytes / 1024).toFixed(1)} KB → <strong>{(ev.compressedSizeBytes / 1024).toFixed(1)} KB</strong>
                    </span>
                  </div>
                  {photoUrls[ev.id] ? (
                    <img src={photoUrls[ev.id]} alt="Evidencia" style={{ width: '100%', borderRadius: 6, maxHeight: 220, objectFit: 'cover' }} />
                  ) : (
                    <button className="btn btn-sm" onClick={() => handlePreviewPhoto(ev.id)}>Cargar Imagen desde MinIO</button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {tab === 'actas' && !isCommon && (
          <div style={{ display: 'grid', gap: 14 }}>
            <form onSubmit={handleGenerateActa} className="card form-grid">
              <div className="field">
                <label>Tipo de Acta Oficial</label>
                <select className="select" value={actaForm.documentType} onChange={(e) => setActaForm({ ...actaForm, documentType: e.target.value as DocumentType })}>
                  <option value="ASIGNACION">Acta de Asignación</option>
                  <option value="SALIDA">Acta de Salida</option>
                  <option value="ENTRADA">Acta de Entrada</option>
                  <option value="REPARACION">Acta de Reparación</option>
                  <option value="TRANSPORTE">Acta de Transporte</option>
                </select>
              </div>
              <div className="field">
                <label>Motivo</label>
                <input className="input" value={actaForm.reason} onChange={(e) => setActaForm({ ...actaForm, reason: e.target.value })} required />
              </div>
              <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
                <button className="btn btn-primary" type="submit"><FileText size={14} /> Generar PDF</button>
              </div>
            </form>

            <table className="data-table">
              <thead><tr><th>Folio</th><th>Tipo</th><th>Motivo</th><th>Fecha</th><th>Acción</th></tr></thead>
              <tbody>
                {detail.generatedDocuments.length === 0 ? <tr><td colSpan={5}>Sin actas generadas</td></tr> :
                  detail.generatedDocuments.map((d: any) => (
                    <tr key={d.id}>
                      <td><strong>{d.documentNumber}</strong></td>
                      <td>{d.documentType}</td>
                      <td>{d.reason}</td>
                      <td>{new Date(d.generatedAt).toLocaleString()}</td>
                      <td>
                        <button className="btn btn-sm" onClick={() => handleDownloadPdf(d.id)}>Abrir PDF</button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}

        {tab === 'vault' && isPrivileged && (
          <div className="card">
            <div className="alert alert-info" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <ShieldAlert size={16} />
              <span>Acceso auditado ISO 27001. Cifrado AES-256-GCM gestionado por <code>globalr-sat-crypto</code>.</span>
            </div>
            {vault !== null && (
              <form onSubmit={handleSaveVault} className="form-grid">
                <div className="field">
                  <label>Clave de Recuperación BitLocker</label>
                  <input className="input" value={vaultForm.bitlockerRecoveryKey} onChange={(e) => setVaultForm({ ...vaultForm, bitlockerRecoveryKey: e.target.value })} />
                </div>
                <div className="field">
                  <label>Correo Asociado al Equipo</label>
                  <input className="input" value={vaultForm.deviceEmail} onChange={(e) => setVaultForm({ ...vaultForm, deviceEmail: e.target.value })} />
                </div>
                <div className="field">
                  <label>Contraseña del Equipo</label>
                  <input className="input" value={vaultForm.devicePassword} onChange={(e) => setVaultForm({ ...vaultForm, devicePassword: e.target.value })} />
                </div>
                <div className="field">
                  <label>Notas de Recuperación</label>
                  <input className="input" value={vaultForm.extraNotes} onChange={(e) => setVaultForm({ ...vaultForm, extraNotes: e.target.value })} />
                </div>
                <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
                  <button className="btn btn-primary" type="submit">Guardar en Bóveda Cifrada</button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
