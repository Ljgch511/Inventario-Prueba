import React, { useEffect, useState } from 'react';
import { CheckCircle2, FileText, Plus, QrCode, Wrench } from 'lucide-react';
import { api } from '../api/client';
import { AssetSummary, PersonnelItem, Role, StoredPartItem } from '../types';

export const MaintenanceAndPartsPanel: React.FC<{ role: Role }> = ({ role }) => {
  const [maintenances, setMaintenances] = useState<any[]>([]);
  const [parts, setParts] = useState<StoredPartItem[]>([]);
  const [completing, setCompleting] = useState<any | null>(null);
  const [completeForm, setCompleteForm] = useState({
    workPerformed: '',
    resultingAssetStatus: 'DISPONIBLE',
    storedPartId: '',
    removedPartDesc: '',
    partChangeReason: ''
  });
  const [partForm, setPartForm] = useState({
    partType: 'RAM',
    capacity: '',
    brand: '',
    model: '',
    serialNumber: '',
    physicalLocation: 'Almacén TI'
  });
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const canAct = role === 'ADMIN' || role === 'SUPERVISOR' || role === 'MAINTENANCE';

  const load = async () => {
    try {
      const [m, p] = await Promise.all([api.listMaintenances(), api.listParts()]);
      setMaintenances(m);
      setParts(p);
    } catch (e: any) {
      setErr(e.message);
    }
  };

  useEffect(() => { load(); }, []);

  const handleCompleteMaint = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!completing) return;
    try {
      await api.completeMaintenance(completing.id, {
        workPerformed: completeForm.workPerformed,
        resultingAssetStatus: completeForm.resultingAssetStatus,
        storedPartId: completeForm.storedPartId || null,
        removedPartDesc: completeForm.removedPartDesc || null,
        partChangeReason: completeForm.partChangeReason || null
      });
      setMsg('Mantenimiento finalizado y trazabilidad de partes actualizada.');
      setCompleting(null);
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  const handleAddPart = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.createPart(partForm);
      setPartForm({ partType: 'RAM', capacity: '', brand: '', model: '', serialNumber: '', physicalLocation: 'Almacén TI' });
      setMsg('Parte almacenada registrada.');
      await load();
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      {msg && <div className="alert alert-info">{msg}</div>}
      {err && <div className="alert alert-error">{err}</div>}

      <div className="card table-wrap">
        <h3 className="card-title"><Wrench size={16} style={{ marginRight: 6 }} /> Órdenes de Mantenimiento</h3>
        <table className="data-table">
          <thead><tr><th>Fecha</th><th>Modo</th><th>Técnico/Proveedor</th><th>Falla</th><th>Trabajo Realizado</th><th>Estado</th><th>Acción</th></tr></thead>
          <tbody>
            {maintenances.map((m) => (
              <tr key={m.id}>
                <td>{new Date(m.startedAt).toLocaleDateString()}</td>
                <td>{m.maintenanceMode}</td>
                <td>{m.technicianName || m.providerName || '-'}</td>
                <td>{m.failureDescription}</td>
                <td>{m.workPerformed || '-'}</td>
                <td><span className={`badge ${m.status === 'FINALIZADA' ? 'badge-success' : 'badge-warn'}`}>{m.status}</span></td>
                <td>
                  {canAct && m.status !== 'FINALIZADA' && (
                    <button className="btn btn-primary btn-sm" onClick={() => setCompleting(m)}>
                      <CheckCircle2 size={13} /> Declarar Reparación
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card" style={{ display: 'grid', gap: 12 }}>
        <h3 className="card-title" style={{ marginBottom: 0 }}>Partes Almacenadas (RAM / SSD / Repuestos)</h3>
        {canAct && (
          <form onSubmit={handleAddPart} className="form-grid">
            <div className="field">
              <label>Tipo</label>
              <select className="select" value={partForm.partType} onChange={(e) => setPartForm({ ...partForm, partType: e.target.value })}>
                <option value="RAM">RAM</option>
                <option value="SSD">SSD</option>
                <option value="HDD">HDD</option>
                <option value="OTRO">Otro</option>
              </select>
            </div>
            <div className="field"><label>Capacidad</label><input className="input" value={partForm.capacity} onChange={(e) => setPartForm({ ...partForm, capacity: e.target.value })} placeholder="16 GB DDR5" required /></div>
            <div className="field"><label>Marca</label><input className="input" value={partForm.brand} onChange={(e) => setPartForm({ ...partForm, brand: e.target.value })} required /></div>
            <div className="field"><label>Serial</label><input className="input" value={partForm.serialNumber} onChange={(e) => setPartForm({ ...partForm, serialNumber: e.target.value })} required /></div>
            <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
              <button className="btn btn-primary" type="submit"><Plus size={14} /> Agregar Parte</button>
            </div>
          </form>
        )}
        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Tipo</th><th>Capacidad</th><th>Marca</th><th>Serial</th><th>Ubicación</th><th>Estado</th></tr></thead>
            <tbody>
              {parts.map((p) => (
                <tr key={p.id}>
                  <td><strong>{p.partType}</strong></td><td>{p.capacity}</td><td>{p.brand}</td><td>{p.serialNumber}</td>
                  <td>{p.physicalLocation}</td>
                  <td><span className={`badge ${p.status === 'EN_ALMACEN' ? 'badge-success' : 'badge-neutral'}`}>{p.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {completing && (
        <div className="modal-backdrop">
          <div className="modal-card" style={{ maxWidth: 560 }}>
            <div className="modal-header">
              <h3 style={{ margin: 0 }}>Declarar Trabajo de Mantenimiento</h3>
              <button className="btn btn-sm" onClick={() => setCompleting(null)}>Cerrar</button>
            </div>
            <form onSubmit={handleCompleteMaint} style={{ display: 'grid', gap: 12 }}>
              <div className="field">
                <label>Trabajo Realizado</label>
                <textarea className="textarea" value={completeForm.workPerformed} onChange={(e) => setCompleteForm({ ...completeForm, workPerformed: e.target.value })} required />
              </div>
              <div className="form-grid">
                <div className="field">
                  <label>Estado Resultante del Activo</label>
                  <select className="select" value={completeForm.resultingAssetStatus} onChange={(e) => setCompleteForm({ ...completeForm, resultingAssetStatus: e.target.value })}>
                    <option value="DISPONIBLE">Disponible</option>
                    <option value="ASIGNADO">Asignado (volver al usuario)</option>
                    <option value="DANADO">Dañado (irreparable)</option>
                  </select>
                </div>
                <div className="field">
                  <label>Instalar Parte de Almacén (Opcional)</label>
                  <select className="select" value={completeForm.storedPartId} onChange={(e) => setCompleteForm({ ...completeForm, storedPartId: e.target.value })}>
                    <option value="">Ninguna parte cambiada</option>
                    {parts.filter((p) => p.status === 'EN_ALMACEN').map((p) => (
                      <option key={p.id} value={p.id}>{p.partType} {p.capacity} {p.brand} ({p.serialNumber})</option>
                    ))}
                  </select>
                </div>
              </div>
              {completeForm.storedPartId && (
                <div className="field">
                  <label>Parte Retirada / Motivo</label>
                  <input className="input" value={completeForm.removedPartDesc} onChange={(e) => setCompleteForm({ ...completeForm, removedPartDesc: e.target.value })} placeholder="Ej. SSD 256GB defectuoso" />
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                <button className="btn" type="button" onClick={() => setCompleting(null)}>Cancelar</button>
                <button className="btn btn-primary" type="submit">Finalizar Mantenimiento</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export const MovementsAndDocsPanel: React.FC = () => {
  const [movements, setMovements] = useState<any[]>([]);
  const [docs, setDocs] = useState<any[]>([]);

  useEffect(() => {
    Promise.all([api.listMovements(), api.listDocuments()]).then(([m, d]) => {
      setMovements(m);
      setDocs(d);
    });
  }, []);

  const openPdf = async (docId: string) => {
    const url = await api.fetchBlobUrl(`/documents/${docId}/download`);
    window.open(url, '_blank');
  };

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div className="card table-wrap">
        <h3 className="card-title">Registro de Entradas, Salidas y Transporte</h3>
        <table className="data-table">
          <thead><tr><th>Fecha</th><th>Tipo</th><th>Fuente</th><th>Origen → Destino</th><th>Transportista</th><th>Motivo</th></tr></thead>
          <tbody>
            {movements.length === 0 ? <tr><td colSpan={6}>Sin movimientos registrados</td></tr> :
              movements.map((m) => (
                <tr key={m.id}>
                  <td>{new Date(m.movedAt).toLocaleString()}</td>
                  <td><span className="badge badge-info">{m.movementType}</span></td>
                  <td>{m.entrySource || '-'}</td>
                  <td>{m.originLocation || '-'} → {m.destinationLocation}</td>
                  <td>{m.transporterName || '-'}</td>
                  <td>{m.reason}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <div className="card table-wrap">
        <h3 className="card-title"><FileText size={16} style={{ marginRight: 6 }} /> Archivo de Actas Oficiales (PDF)</h3>
        <table className="data-table">
          <thead><tr><th>Folio</th><th>Tipo</th><th>Motivo</th><th>Fecha</th><th>Archivo</th></tr></thead>
          <tbody>
            {docs.length === 0 ? <tr><td colSpan={5}>Sin actas emitidas</td></tr> :
              docs.map((d) => (
                <tr key={d.id}>
                  <td><strong>{d.documentNumber}</strong></td>
                  <td>{d.documentType}</td>
                  <td>{d.reason}</td>
                  <td>{new Date(d.generatedAt).toLocaleString()}</td>
                  <td><button className="btn btn-sm" onClick={() => openPdf(d.id)}>Ver PDF</button></td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const PersonnelPanel: React.FC<{ role: Role }> = ({ role }) => {
  const [list, setList] = useState<PersonnelItem[]>([]);
  const [form, setForm] = useState({
    targetType: 'PERSON', firstName: '', lastName: '', nationalId: '', groupOrOfficeName: '', email: '', department: ''
  });
  const canEdit = role === 'ADMIN' || role === 'SUPERVISOR';

  const load = () => api.listPersonnel().then(setList);
  useEffect(() => { load(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.createPersonnel(form);
    setForm({ targetType: 'PERSON', firstName: '', lastName: '', nationalId: '', groupOrOfficeName: '', email: '', department: '' });
    load();
  };

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      {canEdit && (
        <form onSubmit={handleCreate} className="card form-grid">
          <div className="field">
            <label>Tipo Destinatario</label>
            <select className="select" value={form.targetType} onChange={(e) => setForm({ ...form, targetType: e.target.value })}>
              <option value="PERSON">Persona</option>
              <option value="OFFICE">Oficina (ej. Cámaras)</option>
              <option value="GROUP">Grupo (ej. Recepción)</option>
            </select>
          </div>
          {form.targetType === 'PERSON' ? (
            <>
              <div className="field"><label>Nombre</label><input className="input" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required /></div>
              <div className="field"><label>Apellido</label><input className="input" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required /></div>
              <div className="field"><label>Cédula</label><input className="input" value={form.nationalId} onChange={(e) => setForm({ ...form, nationalId: e.target.value })} required /></div>
            </>
          ) : (
            <div className="field"><label>Nombre Oficina / Grupo</label><input className="input" value={form.groupOrOfficeName} onChange={(e) => setForm({ ...form, groupOrOfficeName: e.target.value })} required /></div>
          )}
          <div className="field"><label>Departamento</label><input className="input" value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} /></div>
          <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button className="btn btn-primary" type="submit"><Plus size={14} /> Registrar</button>
          </div>
        </form>
      )}
      <div className="card table-wrap">
        <table className="data-table">
          <thead><tr><th>Tipo</th><th>Nombre / Etiqueta</th><th>Cédula</th><th>Departamento</th><th>Email</th></tr></thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.id}>
                <td><span className="badge badge-neutral">{p.targetType}</span></td>
                <td><strong>{p.targetType === 'PERSON' ? `${p.firstName} ${p.lastName}` : p.groupOrOfficeName}</strong></td>
                <td>{p.nationalId || '-'}</td>
                <td>{p.department || '-'}</td>
                <td>{p.email || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const FinanceAndLicensesPanel: React.FC = () => {
  const [records, setRecords] = useState<any[]>([]);
  const [licenses, setLicenses] = useState<any[]>([]);
  const [assets, setAssets] = useState<AssetSummary[]>([]);
  const [finForm, setFinForm] = useState({
    assetId: '', acquisitionType: 'COMPRA_NACIONAL', localCost: '0', localCurrency: 'VES',
    usdReferenceCost: '0', providerName: '', invoiceNumber: '', warrantyMonths: '12',
    leaseCounterparty: '', consignmentInstallments: ''
  });
  const [licForm, setLicForm] = useState({
    osName: 'Windows 11', edition: 'Pro', licenseType: 'VOLUMEN', licenseKeyMasked: '', totalSeats: '5', providerName: '', costUsd: '500'
  });

  const load = async () => {
    const [f, l, a] = await Promise.all([api.listFinancial(), api.listLicenses(), api.listAssets()]);
    setRecords(f);
    setLicenses(l);
    setAssets(a);
    if (!finForm.assetId && a.length > 0) setFinForm((p) => ({ ...p, assetId: a[0].id }));
  };

  useEffect(() => { load(); }, []);

  const handleAddFin = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.createFinancial({
      ...finForm,
      localCost: Number(finForm.localCost),
      usdReferenceCost: Number(finForm.usdReferenceCost),
      warrantyMonths: Number(finForm.warrantyMonths) || null,
      consignmentInstallments: finForm.consignmentInstallments ? Number(finForm.consignmentInstallments) : null
    });
    load();
  };

  const handleAddLic = async (e: React.FormEvent) => {
    e.preventDefault();
    await api.createLicense({
      ...licForm,
      totalSeats: Number(licForm.totalSeats),
      costUsd: Number(licForm.costUsd)
    });
    load();
  };

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <div className="card" style={{ display: 'grid', gap: 12 }}>
        <h3 className="card-title" style={{ marginBottom: 0 }}>Registro Contable, Garantías, Arrendamientos y Consignación</h3>
        <form onSubmit={handleAddFin} className="form-grid">
          <div className="field">
            <label>Activo</label>
            <select className="select" value={finForm.assetId} onChange={(e) => setFinForm({ ...finForm, assetId: e.target.value })} required>
              {assets.map((a) => <option key={a.id} value={a.id}>{a.assetNumber} — {a.brand}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Modalidad</label>
            <select className="select" value={finForm.acquisitionType} onChange={(e) => setFinForm({ ...finForm, acquisitionType: e.target.value })}>
              <option value="COMPRA_NACIONAL">Compra Nacional</option>
              <option value="COMPRA_INTERNACIONAL">Compra Internacional</option>
              <option value="LEASING">Leasing</option>
              <option value="ALQUILER">Alquiler</option>
              <option value="ARRENDAMIENTO_OTORGADO">Arrendamiento (A quién se arrienda)</option>
              <option value="ARRENDAMIENTO_RECIBIDO">Arrendamiento (De quién se arrienda)</option>
              <option value="CONSIGNACION">Consignación</option>
              <option value="REGALO_PROVEEDOR">Regalo Proveedor</option>
            </select>
          </div>
          <div className="field"><label>Costo Local</label><input className="input" type="number" step="0.01" value={finForm.localCost} onChange={(e) => setFinForm({ ...finForm, localCost: e.target.value })} /></div>
          <div className="field"><label>Moneda</label><input className="input" value={finForm.localCurrency} onChange={(e) => setFinForm({ ...finForm, localCurrency: e.target.value })} /></div>
          <div className="field"><label>Ref. USD</label><input className="input" type="number" step="0.01" value={finForm.usdReferenceCost} onChange={(e) => setFinForm({ ...finForm, usdReferenceCost: e.target.value })} /></div>
          <div className="field"><label>Proveedor / Contraparte</label><input className="input" value={finForm.providerName} onChange={(e) => setFinForm({ ...finForm, providerName: e.target.value })} /></div>
          <div className="field"><label>Factura</label><input className="input" value={finForm.invoiceNumber} onChange={(e) => setFinForm({ ...finForm, invoiceNumber: e.target.value })} /></div>
          <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button className="btn btn-primary" type="submit">Registrar Ficha</button>
          </div>
        </form>

        <div className="table-wrap">
          <table className="data-table">
            <thead><tr><th>Modalidad</th><th>Costo Local</th><th>Ref. USD</th><th>Proveedor / Contraparte</th><th>Factura</th><th>Garantía</th></tr></thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}>
                  <td><span className="badge badge-info">{r.acquisitionType}</span></td>
                  <td>{r.localCost} {r.localCurrency}</td>
                  <td><strong>{r.usdReferenceCost} USD</strong></td>
                  <td>{r.providerName || r.leaseCounterparty || '-'}</td>
                  <td>{r.invoiceNumber || '-'}</td>
                  <td>{r.warrantyMonths ? `${r.warrantyMonths} meses` : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card" style={{ display: 'grid', gap: 12 }}>
        <h3 className="card-title" style={{ marginBottom: 0 }}>Licencias de Sistema Operativo Compradas</h3>
        <form onSubmit={handleAddLic} className="form-grid">
          <div className="field"><label>SO</label><input className="input" value={licForm.osName} onChange={(e) => setLicForm({ ...licForm, osName: e.target.value })} required /></div>
          <div className="field"><label>Edición</label><input className="input" value={licForm.edition} onChange={(e) => setLicForm({ ...licForm, edition: e.target.value })} required /></div>
          <div className="field"><label>Tipo</label><input className="input" value={licForm.licenseType} onChange={(e) => setLicForm({ ...licForm, licenseType: e.target.value })} required /></div>
          <div className="field"><label>Clave Enmascarada</label><input className="input" value={licForm.licenseKeyMasked} onChange={(e) => setLicForm({ ...licForm, licenseKeyMasked: e.target.value })} /></div>
          <div className="field"><label>Puestos</label><input className="input" type="number" value={licForm.totalSeats} onChange={(e) => setLicForm({ ...licForm, totalSeats: e.target.value })} /></div>
          <div className="field" style={{ display: 'flex', alignItems: 'flex-end' }}>
            <button className="btn btn-primary" type="submit">Agregar Licencia</button>
          </div>
        </form>
        <table className="data-table">
          <thead><tr><th>SO</th><th>Edición</th><th>Tipo</th><th>Clave</th><th>Puestos</th><th>Costo USD</th></tr></thead>
          <tbody>
            {licenses.map((l) => (
              <tr key={l.id}>
                <td><strong>{l.osName}</strong></td><td>{l.edition}</td><td>{l.licenseType}</td>
                <td><code>{l.licenseKeyMasked || '-'}</code></td><td>{l.totalSeats}</td><td>{l.costUsd} USD</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export const QrScannerPanel: React.FC<{ onOpenAsset: (id: string) => void }> = ({ onOpenAsset }) => {
  const [payload, setPayload] = useState('');
  const [result, setResult] = useState<any>(null);
  const [err, setErr] = useState<string | null>(null);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setErr(null);
      const res = await api.scanQr(payload.trim());
      setResult(res);
    } catch (ex: any) {
      setResult(null);
      setErr(ex.message);
    }
  };

  return (
    <div className="card" style={{ maxWidth: 680 }}>
      <h3 className="card-title"><QrCode size={16} style={{ marginRight: 6 }} /> Validador de Códigos QR Cifrados (sat-crypto &lt; 1s)</h3>
      <p style={{ color: '#64748b', fontSize: 13 }}>
        Pegue aquí un token cifrado <code>GLR1...</code> copiado de cualquier activo (o escaneado desde la APK Android) para validar su firma RSA y descifrarlo respetando aislamiento por país.
      </p>
      <form onSubmit={handleVerify} style={{ display: 'grid', gap: 10 }}>
        <textarea
          className="textarea"
          value={payload}
          onChange={(e) => setPayload(e.target.value)}
          placeholder="GLR1...."
          required
        />
        <button className="btn btn-primary" type="submit" style={{ width: 'fit-content' }}>Verificar y Descifrar</button>
      </form>
      {err && <div className="alert alert-error" style={{ marginTop: 12 }}>{err}</div>}
      {result && (
        <div className="alert alert-info" style={{ marginTop: 12 }}>
          <div><strong>Verificación Exitosa</strong> · Latencia Criptográfica: <strong>{result.processingMs} ms</strong></div>
          <div style={{ marginTop: 6 }}>
            Activo: <strong>{result.asset.assetNumber}</strong> — {result.asset.brand} {result.asset.model} ({result.asset.status})
          </div>
          <button className="btn btn-primary btn-sm" style={{ marginTop: 8 }} onClick={() => onOpenAsset(result.asset.id)}>
            Abrir Ficha del Activo
          </button>
        </div>
      )}
    </div>
  );
};

export const AuditLogsPanel: React.FC = () => {
  const [logs, setLogs] = useState<any[]>([]);
  useEffect(() => { api.listAuditLogs().then(setLogs); }, []);

  return (
    <div className="card table-wrap">
      <h3 className="card-title">Bitácora de Auditoría Inmutable (ISO 27001 / LOPCYMAT)</h3>
      <table className="data-table">
        <thead><tr><th>Fecha</th><th>Actor</th><th>Rol</th><th>Canal</th><th>Acción</th><th>Entidad</th><th>Detalle</th></tr></thead>
        <tbody>
          {logs.map((l) => (
            <tr key={l.id}>
              <td>{new Date(l.createdAt).toLocaleString()}</td>
              <td><strong>{l.actorUsername}</strong></td>
              <td>{l.actorRole}</td>
              <td><span className="badge badge-neutral">{l.sourceChannel}</span></td>
              <td><span className="badge badge-info">{l.action}</span></td>
              <td>{l.entityType}</td>
              <td><code>{JSON.stringify(l.detailsJson)}</code></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
