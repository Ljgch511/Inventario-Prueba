import React, { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { api } from '../api/client';
import { LoginResponse } from '../types';

interface Props {
  onSuccess: (res: LoginResponse) => void;
}

const DEMO_ACCOUNTS = [
  { label: 'Admin VE (Multi-país)', user: 'admin.ve' },
  { label: 'Supervisor VE', user: 'supervisor.ve' },
  { label: 'Usuario Común VE', user: 'usuario.ve' },
  { label: 'Mantenimiento VE', user: 'mantenimiento.ve' },
  { label: 'Contabilidad VE', user: 'contabilidad.ve' },
  { label: 'Admin CO (Aislado)', user: 'admin.co' }
];

export const LoginView: React.FC<Props> = ({ onSuccess }) => {
  const [username, setUsername] = useState('admin.ve');
  const [password, setPassword] = useState('GlobalR2026!');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = (await api.login(username, password)) as LoginResponse;
      api.setSession(res.token, res.user.countryCode);
      onSuccess(res);
    } catch (err: any) {
      setError(err.message || 'Error iniciando sesión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
          <ShieldCheck size={28} color="#0f4c81" />
          <div>
            <h2 style={{ margin: 0, fontSize: 19, color: '#0f172a' }}>GlobalR — Inventario ITSM</h2>
            <div style={{ fontSize: 12, color: '#64748b' }}>
              Arquitectura Híbrida (Spring Boot + Quarkus) · ISO 27001 / LOPCYMAT
            </div>
          </div>
        </div>

        {error && <div className="alert alert-error" style={{ marginTop: 12 }}>{error}</div>}

        <form onSubmit={handleSubmit} style={{ marginTop: 16, display: 'grid', gap: 12 }}>
          <div className="field">
            <label>Usuario</label>
            <input
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="ej. supervisor.ve"
              required
            />
          </div>
          <div className="field">
            <label>Contraseña</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ padding: '10px 14px' }}>
            {loading ? 'Validando...' : 'Iniciar Sesión'}
          </button>
        </form>

        <div style={{ marginTop: 18, paddingTop: 14, borderTop: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: 11.5, color: '#64748b', fontWeight: 600 }}>
            Perfiles de prueba (clave: <code>GlobalR2026!</code>):
          </div>
          <div className="demo-pills">
            {DEMO_ACCOUNTS.map((acc) => (
              <button
                key={acc.user}
                type="button"
                className="btn btn-sm"
                onClick={() => {
                  setUsername(acc.user);
                  setPassword('GlobalR2026!');
                }}
              >
                {acc.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
