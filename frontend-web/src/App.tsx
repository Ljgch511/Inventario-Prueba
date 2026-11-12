import React, { useEffect, useState } from 'react';
import {
  Activity, Box, ClipboardList, DollarSign, FileSpreadsheet, Globe, Laptop,
  LogOut, Mail, Plus, QrCode, Search, Shield, Users, Wrench, X
} from 'lucide-react';
import { api } from './api/client';
import { AssetCategory, AssetStatus, AssetSummary, CountryDto, LoginResponse, UserSummary } from './types';
import { LoginView } from './components/LoginView';
import { AssetDetailPanel } from './components/AssetDetailPanel';
import { TicketsWorkspace } from './components/TicketsWorkspace';
import {
  AuditLogsPanel, FinanceAndLicensesPanel, MaintenanceAndPartsPanel,
  MovementsAndDocsPanel, PersonnelPanel, QrScannerPanel
} from './components/OperationsPanels';

type NavKey =
  | 'dashboard'
  | 'assets'
  | 'tickets'
  | 'maintenance'
  | 'movements'
  | 'personnel'
  | 'finance'
  | 'qr'
  | 'audit';

const assetStatusBadge = (s: AssetStatus) => {
  switch (s) {
    case 'DISPONIBLE': return 'badge badge-success';
    case 'ASIGNADO': return 'badge badge-info';
    case 'EN_MANTENIMIENTO':
    case 'EN_TRANSPORTE': return 'badge badge-warn';
    case 'DANADO':
    case 'DESINCORPORADO': return 'badge badge-danger';
  }
};

export const App: React.FC = () => {
  const [user, setUser] = useState<UserSummary | null>(() => {
    const raw = localStorage.getItem('glr_user');
    return raw ? JSON.parse(raw) : null;
  });
  const [countries, setCountries] = useState<CountryDto[]>(() => {
    const raw = localStorage.getItem('glr_countries');
    return raw ? JSON.parse(raw) : [];
  });
  const [activeCountry, setActiveCountry] = useState<string>(() => localStorage.getItem('glr_country') || 'VE');
  const [nav, setNav] = useState<NavKey>('dashboard');

  const [dashboard, setDashboard] = useState<any>(null);
  const [assets, setAssets] = useState<AssetSummary[]>([]);
  const [catFilter, setCatFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchText, setSearchText] = useState<string>('');
  const [selectedAssetId, setSelectedAssetId] = useState<string | null>(null);
  const [ticketAssetId, setTicketAssetId] = useState<string | null>(null);
  const [showCreateAsset, setShowCreateAsset] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [newAsset, setNewAsset] = useState({
    assetNumber: '',
    equipmentNumber: '',
    category: 'ACTIVO' as AssetCategory,
    subcategory: 'LAPTOP',
    brand: '',
    model: '',
    serialNumber: '',
    conditionStatus: 'NUEVO',
    processor: '',
    operatingSystem: '',
    ramSpec: '',
    storageRomSpec: '',
    hasEthernetPort: true,
    physicalLocation: '',
    bitlockerEnabled: false,
    observations: '',
    qualityRating: 5,
    extraSpecKey: '',
    extraSpecVal: '',
    bitlockerRecoveryKey: '',
    deviceEmail: '',
    devicePassword: ''
  });

  const handleLoginSuccess = (res: LoginResponse) => {
    localStorage.setItem('glr_user', JSON.stringify(res.user));
    localStorage.setItem('glr_countries', JSON.stringify(res.availableCountries));
    setUser(res.user);
    setCountries(res.availableCountries);
    setActiveCountry(res.user.countryCode);
    setNav(res.user.role === 'COMMON_USER' ? 'assets' : 'dashboard');
  };

  const handleLogout = () => {
    api.setSession(null, null);
    localStorage.removeItem('glr_user');
    localStorage.removeItem('glr_countries');
    setUser(null);
  };

  const handleSwitchCountry = (code: string) => {
    api.setCountryCode(code);
    setActiveCountry(code);
    setSelectedAssetId(null);
  };

  const loadDashboard = async () => {
    try {
      const d = await api.getDashboard();
      setDashboard(d);
    } catch (e: any) {
      setError(e.message);
    }
  };

  const loadAssets = async () => {
    try {
      setError(null);
      const list = await api.listAssets({
        category: catFilter || undefined,
        status: statusFilter || undefined,
        search: searchText || undefined
      });
      setAssets(list);
    } catch (e: any) {
      setError(e.message);
    }
  };

  useEffect(() => {
    if (!user) return;
    if (nav === 'dashboard') loadDashboard();
    if (nav === 'assets') loadAssets();
  }, [user, nav, activeCountry, catFilter, statusFilter]);

  const handleCreateAsset = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      const specsJson: Record<string, any> = {};
      if (newAsset.extraSpecKey.trim()) {
        specsJson[newAsset.extraSpecKey.trim()] = newAsset.extraSpecVal.trim();
      }
      await api.createAsset({
        assetNumber: newAsset.assetNumber,
        equipmentNumber: newAsset.equipmentNumber || null,
        category: newAsset.category,
        subcategory: newAsset.subcategory,
        brand: newAsset.brand,
        model: newAsset.model,
        serialNumber: newAsset.serialNumber,
        conditionStatus: newAsset.conditionStatus,
        processor: newAsset.category === 'ACTIVO' ? newAsset.processor : null,
        operatingSystem: newAsset.category === 'ACTIVO' ? newAsset.operatingSystem : null,
        ramSpec: newAsset.category === 'ACTIVO' ? newAsset.ramSpec : null,
        storageRomSpec: newAsset.category === 'ACTIVO' ? newAsset.storageRomSpec : null,
        hasEthernetPort: newAsset.category === 'ACTIVO' ? newAsset.hasEthernetPort : null,
        physicalLocation: newAsset.physicalLocation,
        bitlockerEnabled: newAsset.bitlockerEnabled,
        observations: newAsset.observations || null,
        specsJson,
        qualityRating: Number(newAsset.qualityRating),
        bitlockerRecoveryKey: newAsset.bitlockerRecoveryKey || null,
        deviceEmail: newAsset.deviceEmail || null,
        devicePassword: newAsset.devicePassword || null
      });
      setShowCreateAsset(false);
      await loadAssets();
    } catch (ex: any) {
      setError(ex.message);
    }
  };

  if (!user) {
    return <LoginView onSuccess={handleLoginSuccess} />;
  }

  const role = user.role;
  const isPrivileged = role === 'ADMIN' || role === 'SUPERVISOR';
  const isCommon = role === 'COMMON_USER';

  const navItems: { key: NavKey; label: string; icon: React.ReactNode; visible: boolean }[] = [
    { key: 'dashboard', label: 'Dashboard', icon: <Activity size={16} />, visible: !isCommon },
    { key: 'assets', label: isCommon ? 'Mis Activos' : 'Inventario Activos', icon: <Laptop size={16} />, visible: true },
    { key: 'tickets', label: 'Tickets ITSM', icon: <ClipboardList size={16} />, visible: true },
    { key: 'maintenance', label: 'Mantenimiento & Partes', icon: <Wrench size={16} />, visible: isPrivileged || role === 'MAINTENANCE' },
    { key: 'movements', label: 'Movimientos & Actas', icon: <FileSpreadsheet size={16} />, visible: isPrivileged || role === 'MAINTENANCE' },
    { key: 'personnel', label: 'Personal y Oficinas', icon: <Users size={16} />, visible: isPrivileged },
    { key: 'finance', label: 'Contabilidad & Licencias', icon: <DollarSign size={16} />, visible: isPrivileged || role === 'ACCOUNTING' },
    { key: 'qr', label: 'Escáner QR Cifrado', icon: <QrCode size={16} />, visible: !isCommon },
    { key: 'audit', label: 'Auditoría ISO 27001', icon: <Shield size={16} />, visible: isPrivileged }
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h1>GlobalR Inventario</h1>
          <span>Núcleo Spring + Satélites Quarkus</span>
        </div>
        <nav className="nav-list">
          {navItems.filter((n) => n.visible).map((item) => (
            <button
              key={item.key}
              className={`nav-btn ${nav === item.key ? 'active' : ''}`}
              onClick={() => setNav(item.key)}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div style={{ padding: 12, borderTop: '1px solid rgba(255,255,255,0.08)', fontSize: 12 }}>
          <a
            href="/mailpit/"
            target="_blank"
            rel="noreferrer"
            style={{ color: '#93c5fd', display: 'flex', alignItems: 'center', gap: 6, textDecoration: 'none', marginBottom: 8 }}
          >
            <Mail size={14} /> Abrir Bandeja Mailpit (SMTP)
          </a>
          <div style={{ color: '#94a3b8' }}>{user.fullName}</div>
          <div style={{ color: '#64748b', fontSize: 11 }}>{user.role} · {activeCountry}</div>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            <Box size={18} color="#0f4c81" />
            <strong>{navItems.find((i) => i.key === nav)?.label}</strong>
          </div>
          <div className="topbar-right">
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Globe size={15} color="#64748b" />
              {user.multiCountryAccess ? (
                <select
                  className="select"
                  style={{ width: 'auto', padding: '4px 8px' }}
                  value={activeCountry}
                  onChange={(e) => handleSwitchCountry(e.target.value)}
                >
                  {countries.map((c) => (
                    <option key={c.code} value={c.code}>{c.code} — {c.name}</option>
                  ))}
                </select>
              ) : (
                <span className="badge badge-info">País: {activeCountry}</span>
              )}
            </div>
            <button
              className="btn btn-primary btn-sm"
              onClick={() => {
                setTicketAssetId(assets[0]?.id || null);
                setNav('tickets');
              }}
            >
              <Plus size={14} /> Nuevo Ticket
            </button>
            <button className="btn btn-sm" onClick={handleLogout} title="Cerrar sesión">
              <LogOut size={14} /> Salir
            </button>
          </div>
        </header>

        <main className="content-body">
          {error && <div className="alert alert-error">{error}</div>}

          {nav === 'dashboard' && dashboard && (
            <div style={{ display: 'grid', gap: 16 }}>
              <div className="grid-4">
                <div className="stat-card">
                  <div className="stat-label">Total Activos ({dashboard.countryCode})</div>
                  <div className="stat-value">{dashboard.totalAssets}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Disponibles</div>
                  <div className="stat-value" style={{ color: '#15803d' }}>{dashboard.assetsByStatus.DISPONIBLE || 0}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Asignados</div>
                  <div className="stat-value" style={{ color: '#0f4c81' }}>{dashboard.assetsByStatus.ASIGNADO || 0}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Tickets Pendientes Aprobación</div>
                  <div className="stat-value" style={{ color: '#b45309' }}>{dashboard.pendingTickets}</div>
                </div>
              </div>

              <div className="grid-2">
                <div className="card">
                  <h3 className="card-title">Distribución por Estado</h3>
                  <table className="data-table">
                    <thead><tr><th>Estado</th><th>Cantidad</th></tr></thead>
                    <tbody>
                      {Object.entries(dashboard.assetsByStatus).map(([k, v]: any) => (
                        <tr key={k}>
                          <td><span className={assetStatusBadge(k as AssetStatus)}>{k}</span></td>
                          <td><strong>{v}</strong></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div className="card">
                  <h3 className="card-title">Registro de Calidad por Marca</h3>
                  <table className="data-table">
                    <thead><tr><th>Marca</th><th>Equipos</th><th>Incidencias Mant.</th><th>Calificación Prom.</th></tr></thead>
                    <tbody>
                      {dashboard.brandQuality.map((b: any) => (
                        <tr key={b.brand}>
                          <td><strong>{b.brand}</strong></td>
                          <td>{b.totalAssets}</td>
                          <td>{b.maintenanceIncidents}</td>
                          <td><span className="badge badge-info">{b.averageQualityRating} / 5</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {nav === 'assets' && (
            <div style={{ display: 'grid', gap: 16 }}>
              <div className="card" style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', flex: 1 }}>
                  <div style={{ position: 'relative', minWidth: 220, flex: 1, maxWidth: 320 }}>
                    <input
                      className="input"
                      placeholder="Buscar por número, marca, modelo o serial..."
                      value={searchText}
                      onChange={(e) => setSearchText(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && loadAssets()}
                    />
                  </div>
                  <select className="select" style={{ width: 170 }} value={catFilter} onChange={(e) => setCatFilter(e.target.value)}>
                    <option value="">Todas las categorías</option>
                    <option value="ACTIVO">Activos (Laptop/PC)</option>
                    <option value="COMPLEMENTO">Complementos</option>
                    <option value="OBJETO_OFICINA">Objetos de Oficina</option>
                  </select>
                  <select className="select" style={{ width: 170 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                    <option value="">Todos los estados</option>
                    <option value="DISPONIBLE">Disponible</option>
                    <option value="ASIGNADO">Asignado</option>
                    <option value="EN_MANTENIMIENTO">En Mantenimiento</option>
                    <option value="EN_TRANSPORTE">En Transporte</option>
                    <option value="DANADO">Dañado</option>
                    <option value="DESINCORPORADO">Desincorporado</option>
                  </select>
                  <button className="btn" onClick={loadAssets}><Search size={14} /> Filtrar</button>
                </div>
                {isPrivileged && (
                  <button className="btn btn-primary" onClick={() => setShowCreateAsset(true)}>
                    <Plus size={15} /> Registrar Activo
                  </button>
                )}
              </div>

              <div className="card table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>N° Activo</th>
                      <th>Categoría</th>
                      <th>Marca / Modelo</th>
                      <th>Serial</th>
                      <th>Ubicación</th>
                      <th>Asignado a</th>
                      <th>Estado</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {assets.length === 0 ? (
                      <tr><td colSpan={8}>No se encontraron activos en {activeCountry}.</td></tr>
                    ) : assets.map((a) => (
                      <tr key={a.id}>
                        <td><strong>{a.assetNumber}</strong><div style={{ fontSize: 11, color: '#64748b' }}>{a.equipmentNumber || ''}</div></td>
                        <td><span className="badge badge-neutral">{a.category} · {a.subcategory}</span></td>
                        <td>{a.brand} {a.model}</td>
                        <td><code>{a.serialNumber}</code></td>
                        <td>{a.physicalLocation}</td>
                        <td>{a.currentAssigneeLabel || 'Almacén / Sin asignar'}</td>
                        <td><span className={assetStatusBadge(a.status)}>{a.status}</span></td>
                        <td>
                          <div style={{ display: 'flex', gap: 6 }}>
                            <button className="btn btn-sm" onClick={() => setSelectedAssetId(a.id)}>Ver Ficha</button>
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => {
                                setTicketAssetId(a.id);
                                setNav('tickets');
                              }}
                            >
                              Solicitar / Reportar
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {nav === 'tickets' && (
            <TicketsWorkspace
              key={activeCountry}
              role={role}
              preselectedAssetId={ticketAssetId}
              onClearPreselected={() => setTicketAssetId(null)}
            />
          )}

          {nav === 'maintenance' && <MaintenanceAndPartsPanel key={activeCountry} role={role} />}
          {nav === 'movements' && <MovementsAndDocsPanel key={activeCountry} />}
          {nav === 'personnel' && <PersonnelPanel key={activeCountry} role={role} />}
          {nav === 'finance' && <FinanceAndLicensesPanel key={activeCountry} />}
          {nav === 'qr' && <QrScannerPanel onOpenAsset={(id) => setSelectedAssetId(id)} />}
          {nav === 'audit' && <AuditLogsPanel key={activeCountry} />}
        </main>
      </div>

      {selectedAssetId && (
        <AssetDetailPanel
          assetId={selectedAssetId}
          role={role}
          onClose={() => setSelectedAssetId(null)}
          onDeleted={() => {
            setSelectedAssetId(null);
            loadAssets();
          }}
          onQuickTicket={(id) => {
            setSelectedAssetId(null);
            setTicketAssetId(id);
            setNav('tickets');
          }}
        />
      )}

      {showCreateAsset && (
        <div className="modal-backdrop">
          <div className="modal-card">
            <div className="modal-header">
              <h3 style={{ margin: 0 }}>Registrar Nuevo Elemento de Inventario</h3>
              <button className="btn btn-sm" onClick={() => setShowCreateAsset(false)}><X size={16} /></button>
            </div>
            <form onSubmit={handleCreateAsset} style={{ display: 'grid', gap: 12 }}>
              <div className="form-grid">
                <div className="field">
                  <label>Categoría</label>
                  <select
                    className="select"
                    value={newAsset.category}
                    onChange={(e) => {
                      const cat = e.target.value as AssetCategory;
                      setNewAsset({
                        ...newAsset,
                        category: cat,
                        subcategory: cat === 'ACTIVO' ? 'LAPTOP' : cat === 'COMPLEMENTO' ? 'MONITOR' : 'CAMARA'
                      });
                    }}
                  >
                    <option value="ACTIVO">Activo (Laptop / PC)</option>
                    <option value="COMPLEMENTO">Complemento (Celular, UPS, Monitor, RAM...)</option>
                    <option value="OBJETO_OFICINA">Objeto de Oficina (Cámara, TV, Silla, Servidor...)</option>
                  </select>
                </div>
                <div className="field"><label>Subcategoría</label><input className="input" value={newAsset.subcategory} onChange={(e) => setNewAsset({ ...newAsset, subcategory: e.target.value })} required /></div>
                <div className="field"><label>N° Activo</label><input className="input" value={newAsset.assetNumber} onChange={(e) => setNewAsset({ ...newAsset, assetNumber: e.target.value })} placeholder="GLR-VE-0010" required /></div>
                <div className="field"><label>N° Equipo</label><input className="input" value={newAsset.equipmentNumber} onChange={(e) => setNewAsset({ ...newAsset, equipmentNumber: e.target.value })} /></div>
                <div className="field"><label>Marca</label><input className="input" value={newAsset.brand} onChange={(e) => setNewAsset({ ...newAsset, brand: e.target.value })} required /></div>
                <div className="field"><label>Modelo</label><input className="input" value={newAsset.model} onChange={(e) => setNewAsset({ ...newAsset, model: e.target.value })} required /></div>
                <div className="field"><label>Serial</label><input className="input" value={newAsset.serialNumber} onChange={(e) => setNewAsset({ ...newAsset, serialNumber: e.target.value })} required /></div>
                <div className="field">
                  <label>Condición</label>
                  <select className="select" value={newAsset.conditionStatus} onChange={(e) => setNewAsset({ ...newAsset, conditionStatus: e.target.value })}>
                    <option value="NUEVO">Nuevo</option>
                    <option value="BUENO">Bueno</option>
                    <option value="REGULAR">Regular</option>
                    <option value="MALO">Malo</option>
                  </select>
                </div>
                <div className="field"><label>Ubicación Física</label><input className="input" value={newAsset.physicalLocation} onChange={(e) => setNewAsset({ ...newAsset, physicalLocation: e.target.value })} required /></div>
                <div className="field"><label>Calificación Marca (1-5)</label><input className="input" type="number" min={1} max={5} value={newAsset.qualityRating} onChange={(e) => setNewAsset({ ...newAsset, qualityRating: Number(e.target.value) })} /></div>
              </div>

              {newAsset.category === 'ACTIVO' && (
                <div className="card form-grid">
                  <div className="field"><label>Procesador</label><input className="input" value={newAsset.processor} onChange={(e) => setNewAsset({ ...newAsset, processor: e.target.value })} /></div>
                  <div className="field"><label>Sistema Operativo</label><input className="input" value={newAsset.operatingSystem} onChange={(e) => setNewAsset({ ...newAsset, operatingSystem: e.target.value })} /></div>
                  <div className="field"><label>RAM</label><input className="input" value={newAsset.ramSpec} onChange={(e) => setNewAsset({ ...newAsset, ramSpec: e.target.value })} /></div>
                  <div className="field"><label>Almacenamiento (ROM)</label><input className="input" value={newAsset.storageRomSpec} onChange={(e) => setNewAsset({ ...newAsset, storageRomSpec: e.target.value })} /></div>
                  <div className="field">
                    <label>Puerto Ethernet</label>
                    <select className="select" value={String(newAsset.hasEthernetPort)} onChange={(e) => setNewAsset({ ...newAsset, hasEthernetPort: e.target.value === 'true' })}>
                      <option value="true">Sí</option>
                      <option value="false">No</option>
                    </select>
                  </div>
                  <div className="field">
                    <label>BitLocker Activo</label>
                    <select className="select" value={String(newAsset.bitlockerEnabled)} onChange={(e) => setNewAsset({ ...newAsset, bitlockerEnabled: e.target.value === 'true' })}>
                      <option value="false">No</option>
                      <option value="true">Sí</option>
                    </select>
                  </div>
                </div>
              )}

              <div className="card form-grid">
                <div className="field"><label>Atributo Específico JSONB (Clave)</label><input className="input" value={newAsset.extraSpecKey} onChange={(e) => setNewAsset({ ...newAsset, extraSpecKey: e.target.value })} placeholder="ej. resolucion / pulgadas / capacidadVA" /></div>
                <div className="field"><label>Valor Atributo JSONB</label><input className="input" value={newAsset.extraSpecVal} onChange={(e) => setNewAsset({ ...newAsset, extraSpecVal: e.target.value })} placeholder="ej. 4K / 27 / 1200VA" /></div>
              </div>

              {newAsset.category === 'ACTIVO' && (
                <div className="card form-grid">
                  <div className="field"><label>Bóveda: Clave BitLocker (cifrada)</label><input className="input" value={newAsset.bitlockerRecoveryKey} onChange={(e) => setNewAsset({ ...newAsset, bitlockerRecoveryKey: e.target.value })} /></div>
                  <div className="field"><label>Bóveda: Correo Equipo (cifrado)</label><input className="input" value={newAsset.deviceEmail} onChange={(e) => setNewAsset({ ...newAsset, deviceEmail: e.target.value })} /></div>
                  <div className="field"><label>Bóveda: Contraseña (cifrada)</label><input className="input" type="password" value={newAsset.devicePassword} onChange={(e) => setNewAsset({ ...newAsset, devicePassword: e.target.value })} /></div>
                </div>
              )}

              <div className="field">
                <label>Observaciones Operativas</label>
                <textarea className="textarea" value={newAsset.observations} onChange={(e) => setNewAsset({ ...newAsset, observations: e.target.value })} />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                <button className="btn" type="button" onClick={() => setShowCreateAsset(false)}>Cancelar</button>
                <button className="btn btn-primary" type="submit">Guardar y Generar QR</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
