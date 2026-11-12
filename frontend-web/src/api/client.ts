const API_BASE = '/api/v1';

class ApiClient {
  private token: string | null = localStorage.getItem('glr_token');
  private countryCode: string | null = localStorage.getItem('glr_country');

  setSession(token: string | null, countryCode: string | null) {
    this.token = token;
    this.countryCode = countryCode;
    if (token) localStorage.setItem('glr_token', token);
    else localStorage.removeItem('glr_token');
    if (countryCode) localStorage.setItem('glr_country', countryCode);
    else localStorage.removeItem('glr_country');
  }

  setCountryCode(code: string) {
    this.countryCode = code;
    localStorage.setItem('glr_country', code);
  }

  private headers(extra: Record<string, string> = {}): Record<string, string> {
    const h: Record<string, string> = {
      'X-Client-Channel': 'WEB',
      ...extra
    };
    if (this.token) h['Authorization'] = `Bearer ${this.token}`;
    if (this.countryCode) h['X-Country-Code'] = this.countryCode;
    return h;
  }

  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const isForm = init.body instanceof FormData;
    const headers = this.headers(
      isForm ? {} : { 'Content-Type': 'application/json' }
    );
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { ...headers, ...(init.headers as Record<string, string> | undefined) }
    });

    if (!res.ok) {
      let msg = `Error ${res.status}`;
      try {
        const body = await res.json();
        if (body?.message) msg = body.message;
      } catch {
        // ignore parse error
      }
      throw new Error(msg);
    }
    if (res.status === 204) return undefined as T;
    return res.json() as Promise<T>;
  }

  async fetchBlobUrl(path: string): Promise<string> {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: this.headers()
    });
    if (!res.ok) throw new Error(`No se pudo descargar el recurso (${res.status})`);
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  }

  login(username: string, password: string) {
    return this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
  }

  getDashboard() {
    return this.request<any>('/dashboard');
  }

  listAssets(params: { category?: string; status?: string; search?: string } = {}) {
    const qs = new URLSearchParams();
    if (params.category) qs.set('category', params.category);
    if (params.status) qs.set('status', params.status);
    if (params.search) qs.set('search', params.search);
    const suffix = qs.toString() ? `?${qs.toString()}` : '';
    return this.request<any[]>(`/assets${suffix}`);
  }

  getAssetDetail(id: string) {
    return this.request<any>(`/assets/${id}`);
  }

  createAsset(payload: any) {
    return this.request<any>('/assets', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  softDeleteAsset(id: string) {
    return this.request<void>(`/assets/${id}`, { method: 'DELETE' });
  }

  getAssetQr(id: string) {
    return this.request<any>(`/assets/${id}/qr`);
  }

  scanQr(encryptedPayload: string) {
    return this.request<any>('/assets/qr/scan', {
      method: 'POST',
      body: JSON.stringify({ encryptedPayload })
    });
  }

  addComplement(assetId: string, payload: any) {
    return this.request<any>(`/assets/${assetId}/complements`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  getVault(assetId: string) {
    return this.request<any>(`/assets/${assetId}/vault`);
  }

  upsertVault(assetId: string, payload: any) {
    return this.request<any>(`/assets/${assetId}/vault`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  }

  uploadEvidence(assetId: string, stage: string, file: File, ticketId?: string) {
    const fd = new FormData();
    fd.append('file', file);
    const qs = new URLSearchParams({ stage });
    if (ticketId) qs.set('ticketId', ticketId);
    return this.request<any>(`/assets/${assetId}/media?${qs.toString()}`, {
      method: 'POST',
      body: fd
    });
  }

  listTickets(params: { status?: string; type?: string } = {}) {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.type) qs.set('type', params.type);
    const suffix = qs.toString() ? `?${qs.toString()}` : '';
    return this.request<any[]>(`/tickets${suffix}`);
  }

  createTicket(payload: any) {
    return this.request<any>('/tickets', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  approveTicket(id: string, payload: any = {}) {
    return this.request<any>(`/tickets/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  rejectTicket(id: string, rejectionReason: string) {
    return this.request<any>(`/tickets/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ rejectionReason })
    });
  }

  completeTransport(id: string, comment: string) {
    return this.request<any>(`/tickets/${id}/complete-transport`, {
      method: 'POST',
      body: JSON.stringify({ comment })
    });
  }

  listMaintenances() {
    return this.request<any[]>('/maintenances');
  }

  completeMaintenance(id: string, payload: any) {
    return this.request<any>(`/maintenances/${id}/complete`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  generateActa(payload: any) {
    return this.request<any>('/documents/generate', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  listDocuments() {
    return this.request<any[]>('/documents');
  }

  listMovements() {
    return this.request<any[]>('/movements');
  }

  listPersonnel() {
    return this.request<any[]>('/personnel');
  }

  createPersonnel(payload: any) {
    return this.request<any>('/personnel', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  listParts() {
    return this.request<any[]>('/parts');
  }

  createPart(payload: any) {
    return this.request<any>('/parts', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  listFinancial() {
    return this.request<any[]>('/financial');
  }

  createFinancial(payload: any) {
    return this.request<any>('/financial', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  listLicenses() {
    return this.request<any[]>('/licenses');
  }

  createLicense(payload: any) {
    return this.request<any>('/licenses', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  listAuditLogs() {
    return this.request<any[]>('/audit-logs');
  }

  listUsers() {
    return this.request<any[]>('/users');
  }
}

export const api = new ApiClient();
