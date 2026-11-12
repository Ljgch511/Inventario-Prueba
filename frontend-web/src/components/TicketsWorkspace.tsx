import React, { useEffect, useState } from 'react';
import { CheckCircle2, FileText, Plus, Truck, X, XCircle } from 'lucide-react';
import { api } from '../api/client';
import {
  AssetSummary, DocumentType, PersonnelItem, Role, StoredPartItem, TicketRow, TicketStatus, TicketType
} from '../types';

interface Props {
  role: Role;
  preselectedAssetId?: string | null;
  onClearPreselected?: () => void;
}

const ticketBadge = (s: TicketStatus) => {
  switch (s) {
    case 'PENDIENTE_APROBACION': return 'badge badge-warn';
    case 'EN_PROCESO':
    case 'APROBADO': return 'badge badge-info';
    case 'COMPLETADO': return 'badge badge-success';
    case 'RECHAZADO':
    case 'CANCELADO': return 'badge badge-danger';
  }
};

export const TicketsWorkspace: React.FC<Props> = ({ role, preselectedAssetId, onClearPreselected }) => {
  const [tickets, setTickets] = useState<TicketRow[]>([]);
  const [assets, setAssets] = useState<AssetSummary[]>([]);
  const [personnel, setPersonnel] = useState<PersonnelItem[]>([]);
  const [parts, setParts] = useState<StoredPartItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [showCreate, setShowCreate] = useState(Boolean(preselectedAssetId));
  const [rejectModal, setRejectModal] = useState<TicketRow | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const [form, setForm] = useState({
    ticketType: 'MANTENIMIENTO' as TicketType,
    priority: 'MEDIA',
    title: '',
    reason: '',
    assetId: preselectedAssetId || '',
    targetPersonnelId: '',
    transporterName: '',
    transporterIdDoc: '',
    originLocation: '',
    destinationLocation: '',
    disposition: 'DONADO',
    partsUsageNotes: '',
    entrySource: 'REGALO_PROVEEDOR',
    storedPartId: '',
    removedPartDesc: '',
    operationMode: 'CONSIGNACION'
  });

  const canApprove = role === 'ADMIN' || role === 'SUPERVISOR';

  const load = async () => {
    try {
      const [tList, aList, pList, spList] = await Promise.all([
        api.listTickets(statusFilter ? { status: statusFilter } : {}),
        api.listAssets(),
        api.listPersonnel(),
        api.listParts()
      ]);
      setTickets(tList);
      setAssets(aList);
      setPersonnel(pList);
      setParts(spList);
      if (!form.assetId && aList.length > 0) {
        setForm((prev) => ({ ...prev, assetId: preselectedAssetId || aList[0].id }));
      }
    } catch (e: any) {
      setErr(e.message);
    }
  };

  useEffect(() => {
    if (preselectedAssetId) {
      setForm((prev) => ({ ...prev, assetId: preselectedAssetId }));
      setShowCreate(true);
    }
  }, [preselectedAssetId]);

  useEffect(() => {
    load();
  }, [statusFilter]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErr(null);
      const payloadJson: Record<string, any> = {};
      if (form.ticketType === 'DESINCORPORACION') {
        payloadJson.disposition = form.disposition;
        payloadJson.partsUsageNotes = form.partsUsageNotes;
      }
      if (form.ticketType === 'ENTRADA_INGRESO') {
        payloadJson.entrySource = form.entrySource;
      }
      if (form.ticketType === 'CAMBIO_PARTE' && form.storedPartId) {
        payloadJson.storedPartId = form.storedPartId;
        payloadJson.removedPartDesc = form.removedPartDesc;
      }
      if (form.ticketType === 'CONSIGNACION_VENTA') {
        payloadJson.operationMode = form.operationMode;
      }

      const created = await api.createTicket({
        ticketType: form.ticketType,
        priority: form.priority,
        title: form.title,
        reason: form.reason,
        assetId: form.assetId,
        targetPersonnelId: form.targetPersonnelId || null,
        transporterName: form.transporterName || null,
        transporterIdDoc: form.transporterIdDoc || null,
        originLocation: form.originLocation || null,
        destinationLocation: form.destinationLocation || null,
        payloadJson
      });
      setMsg(`Ticket ${created.ticketCode} creado y notificado por correo de forma asíncrona.`);
      setShowCreate(false);
      onClearPreselected?.();
      setForm((prev) => ({ ...prev, title: '', reason: '' }));
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleApprove = async (t: TicketRow) => {
    try {
      setErr(null);
      await api.approveTicket(t.id, { comment: 'Aprobado en consola ITSM' });
      setMsg(`Ticket ${t.ticketCode} aprobado en transacción ACID y notificado.`);
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectModal) return;
    try {
      await api.rejectTicket(rejectModal.id, rejectReason);
      setMsg(`Ticket ${rejectModal.ticketCode} rechazado.`);
      setRejectModal(null);
      setRejectReason('');
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleCompleteTransport = async (t: TicketRow) => {
    try {
      await api.completeTransport(t.id, 'Entrega confirmada en destino');
      setMsg(`Traslado ${t.ticketCode} completado.`);
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleGenerateActaFromTicket = async (t: TicketRow) => {
    const mapType: Record<string, DocumentType> = {
      ASIGNACION: 'ASIGNACION',
      DEVOLUCION: 'ENTRADA',
      ENTRADA_INGRESO: 'ENTRADA',
      SALIDA: 'SALIDA',
      TRASLADO_TRANSPORTE: 'TRANSPORTE',
      MANTENIMIENTO: 'REPARACION'
    };
    const docType = mapType[t.ticketType] || 'ASIGNACION';
    try {
      const doc = await api.generateActa({
        assetId: t.assetId,
        ticketId: t.id,
        documentType: docType,
        reason: t.reason
      });
      setMsg(`Acta ${doc.documentNumber} generada.`);
      const url = await api.fetchBlobUrl(`/documents/${doc.id}/download`);
      window.open(url, '_blank');
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div className="tabs" style={{ marginBottom: 0, borderBottom: 'none' }}>
          {[
            ['', 'Todos'],
            ['PENDIENTE_APROBACION', 'Pendientes Aprobación'],
            ['EN_PROCESO', 'En Proceso'],
            ['COMPLETADO', 'Completados'],
            ['RECHAZADO', 'Rechazados']
          ].map(([val, label]) => (
            <button
              key={val}
              className={`tab-btn ${statusFilter === val ? 'active' : ''}`}
              onClick={() => setStatusFilter(val)}
            >
              {label}
            </button>
          ))}
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
          <Plus size={15} /> Nuevo Ticket
        </button>
      </div>

      {msg && <div className="alert alert-info">{msg}</div>}
      {err && <div className="alert alert-error">{err}</div>}

      <div className="card table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Código</th>
              <th>Tipo</th>
              <th>Título / Motivo</th>
              <th>Activo</th>
              <th>Solicitante</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {tickets.length === 0 ? (
              <tr><td colSpan={7}>No hay tickets para el filtro actual.</td></tr>
            ) : tickets.map((t) => (
              <tr key={t.id}>
                <td><strong>{t.ticketCode}</strong><div style={{ fontSize: 11, color: '#64748b' }}>{t.priority}</div></td>
                <td><span className="badge badge-neutral">{t.ticketType}</span></td>
                <td>
                  <strong>{t.title}</strong>
                  <div style={{ fontSize: 12, color: '#64748b' }}>{t.reason}</div>
                  {t.targetPersonnelLabel && <div style={{ fontSize: 11, color: '#0f4c81' }}>Destino: {t.targetPersonnelLabel}</div>}
                  {t.transporterName && <div style={{ fontSize: 11, color: '#0f4c81' }}>Transportista: {t.transporterName}</div>}
                </td>
                <td><strong>{t.assetNumber}</strong><div style={{ fontSize: 11, color: '#64748b' }}>{t.assetSummary}</div></td>
                <td>{t.requesterName}</td>
                <td>
                  <span className={ticketBadge(t.status)}>{t.status}</span>
                  {t.approverName && <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>Por: {t.approverName}</div>}
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    {canApprove && t.status === 'PENDIENTE_APROBACION' && (
                      <>
                        <button className="btn btn-success btn-sm" onClick={() => handleApprove(t)}>
                          <CheckCircle2 size={13} /> Aprobar
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => setRejectModal(t)}>
                          <XCircle size={13} /> Rechazar
                        </button>
                      </>
                    )}
                    {(role === 'ADMIN' || role === 'SUPERVISOR' || role === 'MAINTENANCE') &&
                      t.status === 'EN_PROCESO' &&
                      (t.ticketType === 'TRASLADO_TRANSPORTE' || t.ticketType === 'SALIDA') && (
                        <button className="btn btn-primary btn-sm" onClick={() => handleCompleteTransport(t)}>
                          <Truck size={13} /> Confirmar Entrega
                        </button>
                      )}
                    {role !== 'COMMON_USER' && (
                      <button className="btn btn-sm" onClick={() => handleGenerateActaFromTicket(t)} title="Generar Acta PDF">
                        <FileText size={13} /> Acta PDF
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3 style={{ margin: 0 }}>Crear Nueva Solicitud (Ticket)</h3>
              <button className="btn btn-sm" onClick={() => { setShowCreate(false); onClearPreselected?.(); }}><X size={16} /></button>
            </div>
            <form onSubmit={handleCreate} style={{ display: 'grid', gap: 12 }}>
              <div className="form-grid">
                <div className="field">
                  <label>Tipo de Solicitud</label>
                  <select className="select" value={form.ticketType} onChange={(e) => setForm({ ...form, ticketType: e.target.value as TicketType })}>
                    <option value="MANTENIMIENTO">Mantenimiento / Falla</option>
                    <option value="ASIGNACION">Asignación (Persona / Oficina / Grupo)</option>
                    <option value="DEVOLUCION">Devolución</option>
                    <option value="TRASLADO_TRANSPORTE">Traslado / Transporte</option>
                    <option value="ENTRADA_INGRESO">Entrada (Compra / Regalo / Garantía)</option>
                    <option value="SALIDA">Salida</option>
                    <option value="CAMBIO_PARTE">Cambio de Parte (Almacén)</option>
                    <option value="DESINCORPORACION">Desincorporación (Donado / Desechado)</option>
                    <option value="CONSIGNACION_VENTA">Consignación / Venta</option>
                  </select>
                </div>
                <div className="field">
                  <label>Activo</label>
                  <select className="select" value={form.assetId} onChange={(e) => setForm({ ...form, assetId: e.target.value })} required>
                    <option value="">Seleccione un activo...</option>
                    {assets.map((a) => (
                      <option key={a.id} value={a.id}>{a.assetNumber} — {a.brand} {a.model} ({a.status})</option>
                    ))}
                  </select>
                </div>
                <div className="field">
                  <label>Prioridad</label>
                  <select className="select" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
                    <option value="BAJA">Baja</option>
                    <option value="MEDIA">Media</option>
                    <option value="ALTA">Alta</option>
                    <option value="CRITICA">Crítica</option>
                  </select>
                </div>
              </div>

              <div className="field">
                <label>Título de la Solicitud</label>
                <input className="input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="Ej. Asignación de laptop o reporte de falla" required />
              </div>
              <div className="field">
                <label>Motivo / Descripción</label>
                <textarea className="textarea" value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required />
              </div>

              {form.ticketType === 'ASIGNACION' && (
                <div className="form-grid">
                  <div className="field">
                    <label>Destinatario (Persona / Oficina / Grupo)</label>
                    <select className="select" value={form.targetPersonnelId} onChange={(e) => setForm({ ...form, targetPersonnelId: e.target.value })} required>
                      <option value="">Seleccione...</option>
                      {personnel.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.targetType === 'PERSON' ? `${p.firstName} ${p.lastName} (${p.nationalId})` : `${p.groupOrOfficeName} [${p.targetType}]`}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <label>Ubicación Destino</label>
                    <input className="input" value={form.destinationLocation} onChange={(e) => setForm({ ...form, destinationLocation: e.target.value })} placeholder="Ej. Piso 3 - Operaciones" />
                  </div>
                </div>
              )}

              {(form.ticketType === 'TRASLADO_TRANSPORTE' || form.ticketType === 'SALIDA') && (
                <div className="form-grid">
                  <div className="field"><label>Transportista</label><input className="input" value={form.transporterName} onChange={(e) => setForm({ ...form, transporterName: e.target.value })} required /></div>
                  <div className="field"><label>Cédula / Doc. Transportista</label><input className="input" value={form.transporterIdDoc} onChange={(e) => setForm({ ...form, transporterIdDoc: e.target.value })} /></div>
                  <div className="field"><label>Ubicación Destino</label><input className="input" value={form.destinationLocation} onChange={(e) => setForm({ ...form, destinationLocation: e.target.value })} required /></div>
                </div>
              )}

              {form.ticketType === 'ENTRADA_INGRESO' && (
                <div className="form-grid">
                  <div className="field">
                    <label>Origen del Ingreso</label>
                    <select className="select" value={form.entrySource} onChange={(e) => setForm({ ...form, entrySource: e.target.value })}>
                      <option value="COMPRA">Compra</option>
                      <option value="REGALO_PROVEEDOR">Regalo de Proveedor</option>
                      <option value="GARANTIA">Garantía</option>
                      <option value="DEVOLUCION">Devolución</option>
                    </select>
                  </div>
                  <div className="field"><label>Ubicación de Ingreso</label><input className="input" value={form.destinationLocation} onChange={(e) => setForm({ ...form, destinationLocation: e.target.value })} required /></div>
                </div>
              )}

              {form.ticketType === 'CAMBIO_PARTE' && (
                <div className="form-grid">
                  <div className="field">
                    <label>Parte Almacenada a Instalar</label>
                    <select className="select" value={form.storedPartId} onChange={(e) => setForm({ ...form, storedPartId: e.target.value })}>
                      <option value="">Seleccione parte en almacén...</option>
                      {parts.filter((p) => p.status === 'EN_ALMACEN').map((p) => (
                        <option key={p.id} value={p.id}>{p.partType} {p.capacity} {p.brand} ({p.serialNumber})</option>
                      ))}
                    </select>
                  </div>
                  <div className="field"><label>Parte Retirada</label><input className="input" value={form.removedPartDesc} onChange={(e) => setForm({ ...form, removedPartDesc: e.target.value })} /></div>
                </div>
              )}

              {form.ticketType === 'DESINCORPORACION' && (
                <div className="form-grid">
                  <div className="field">
                    <label>Disposición</label>
                    <select className="select" value={form.disposition} onChange={(e) => setForm({ ...form, disposition: e.target.value })}>
                      <option value="DONADO">Donado</option>
                      <option value="DESECHADO">Desechado</option>
                    </select>
                  </div>
                  <div className="field"><label>Uso dado a sus partes</label><input className="input" value={form.partsUsageNotes} onChange={(e) => setForm({ ...form, partsUsageNotes: e.target.value })} placeholder="Ej. RAM rescatada para almacén" required /></div>
                </div>
              )}

              {form.ticketType === 'CONSIGNACION_VENTA' && (
                <div className="field">
                  <label>Modalidad Contable</label>
                  <select className="select" value={form.operationMode} onChange={(e) => setForm({ ...form, operationMode: e.target.value })}>
                    <option value="CONSIGNACION">Consignación</option>
                    <option value="VENTA">Venta (Desincorpora activo al aprobar)</option>
                  </select>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 8 }}>
                <button className="btn" type="button" onClick={() => setShowCreate(false)}>Cancelar</button>
                <button className="btn btn-primary" type="submit">Enviar Solicitud</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {rejectModal && (
        <div className="modal-backdrop">
          <div className="modal-card" style={{ maxWidth: 460 }}>
            <div className="modal-header">
              <h3 style={{ margin: 0 }}>Rechazar {rejectModal.ticketCode}</h3>
              <button className="btn btn-sm" onClick={() => setRejectModal(null)}><X size={16} /></button>
            </div>
            <form onSubmit={handleReject} style={{ display: 'grid', gap: 12 }}>
              <div className="field">
                <label>Motivo del Rechazo</label>
                <textarea className="textarea" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} required />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                <button className="btn" type="button" onClick={() => setRejectModal(null)}>Cancelar</button>
                <button className="btn btn-danger" type="submit">Confirmar Rechazo</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
